package com.heishan.jiangshidao.worker;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.entity.GhostWomanEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

// 拆分后的跨类静态导入：方法体保持原样，裸调用在此解析。
import static com.heishan.jiangshidao.worker.WorkerHandler.*;
import static com.heishan.jiangshidao.worker.WorkerFarming.*;
import static com.heishan.jiangshidao.worker.WorkerFishing.*;
import static com.heishan.jiangshidao.worker.WorkerWoodcutting.*;
import static com.heishan.jiangshidao.worker.WorkerMining.*;
import static com.heishan.jiangshidao.worker.WorkerTools.*;
import static com.heishan.jiangshidao.worker.WorkerStorage.*;

/**
 * 工人调度核心：负责扫描、节流、派活与家用回归。
 * <p>具体任务实现已按职责拆到 WorkerFarming / WorkerFishing / WorkerWoodcutting /
 * WorkerMining，工具取用与仓储投递拆到 WorkerTools / WorkerStorage。
 *
 * <p>作者：黑山大叔 —— https://space.bilibili.com/19428259
 */
public final class WorkerHandler {

   /**
    * 工作搜索半径（以铜铃为中心）。工人应能覆盖铜铃周边 20 格内的活儿，
    * 而不只是脚边几格。
    */
   public static final int WORK_RADIUS = 20;
   /**
    * 待命区半径：没活干时回到铜铃这么近的范围内候着，
    * 避免"搜索不到活就四处游荡"。
    */
   static final double IDLE_RADIUS = 4.0;
   static final int INTERVAL = 40;
   static final int BACKPACK_TRANSFER_AT = 20;
   static final int MAX_TRUNK_HEIGHT = 8;
   static final Map<Integer, Long> HINT_AT = new ConcurrentHashMap<>();
   static final Map<LivingEntity, BlockPos> WATER_MEMO = Collections.synchronizedMap(new WeakHashMap<>());
   static final double OPERATE_RANGE = 4.0;
   /** 寻路移动速度（相对基础移动速度的倍数）。 */
   static final double WALK_SPEED = 0.6;
   /** 每 tick 移动的步长（格）。1 秒 20 tick，约合 5 格/秒。 */
   static final double STEP_PER_TICK = 0.25;
   /** 每只工人当前要去的目标方块；移动由 stepTowardTarget 每 tick 执行。 */
   private static final Map<Integer, BlockPos> MOVE_TARGET = new ConcurrentHashMap<>();
   /** 停滞检测：上次位置与连续未移动计数。 */
   private static final Map<Integer, BlockPos> STALL_POS = new ConcurrentHashMap<>();
   private static final Map<Integer, Integer> STALL_COUNT = new ConcurrentHashMap<>();
   /** 连续多少 tick 没移动就判定目标走不到（20 tick = 1 秒）。 */
   static final int STALL_TICKS = 20;
   /** 走不到的目标黑名单：坐标 → 解除时间（gameTime 毫秒基准用 tick）。 */
   private static final Map<BlockPos, Long> BLOCKED = new ConcurrentHashMap<>();
   /** 黑名单保持时长（tick），约 30 秒。 */
   static final long BLOCKED_TICKS = 600L;

   /** 该工人当前正在前往的目标（没有则返回 null）。 */
   static BlockPos currentMoveTarget(LivingEntity worker) {
      return MOVE_TARGET.get(worker.getId());
   }

   /**
    * 该位置是否因走不到而被暂时跳过。
    *
    * <p>各工种选目标时应调用本方法，避免反复选中同一个够不着的方块。
    */
   static boolean isBlocked(BlockPos pos) {
      Long until = BLOCKED.get(pos);
      if (until == null) {
         return false;
      }

      if (until <= currentGameTime) {
         BLOCKED.remove(pos);
         return false;
      }

      return true;
   }

   /** 记录一个走不到的目标。 */
   static void markBlocked(BlockPos pos) {
      BLOCKED.put(pos.immutable(), currentGameTime + BLOCKED_TICKS);
   }

   /** 最近一次可见的服务器时间（由 tickLevel 更新，供静态判定使用）。 */
   private static volatile long currentGameTime;
   /** 闲时回归超时（tick）：这么久没有收成，就回铜铃旁重新开始找活。 */
   static final long IDLE_TIMEOUT_TICKS = 400L;
   /** 每只工人上次"确实收到东西"的时间与背包签名。 */
   private static final Map<Integer, Long> LAST_WORK_AT = new ConcurrentHashMap<>();
   private static final Map<Integer, Integer> LAST_WORK_SIGNATURE = new ConcurrentHashMap<>();
   /** 走到离目标多近就停下（单位：格）。小于操作半径，确保停下后就能操作。 */
   static final double ARRIVE_RADIUS = 1.2;
   /** 距离目标多少格开始线性减速，用来消除过冲。 */
   static final double SLOWDOWN_DISTANCE = 3.0;
   /** 寻路移动速度（相对基础移速的倍数）。 */
   /** 绑定怪物的活动半径（水平，单位：格），以铜铃为中心。 */
   static final double HOME_RADIUS = 20.0;
   static final double HOME_RADIUS_SQR = HOME_RADIUS * HOME_RADIUS;
   static final Map<LivingEntity, WorkerBackpack> BACKPACKS = Collections.synchronizedMap(new WeakHashMap<>());
   private WorkerHandler() {
   }

   public static void tickLevel(ServerLevel level) {
      currentGameTime = level.getGameTime();
      List<Mob> workers = new ArrayList<>();

      for (Entity entity : level.getAllEntities()) {
         if (entity instanceof Mob mob && WorkerData.jobOf(mob) != WorkerJob.IDLE) {
            workers.add(mob);
         }
      }

      for (Mob mob : workers) {
         tickWorker(level, mob);
      }
   }

   static void tickWorker(ServerLevel level, Mob living) {
      try {
         WorkerJob job = WorkerData.jobOf(living);
         if (job == WorkerJob.IDLE) {
            MOVE_TARGET.remove(living.getId());
            return;
         }

         // 工作中必须掐断怪物自身的漫游/跳跃驱动：
         // 僵尸类生物用 tryHop() 沿自身朝向施加跳跃冲量，只要它的漫游目标还在，
         // 就会持续往自己的方向被推走，与我们 stepTowardTarget 的位移叠加后，
         // 表现为"被强行拉向远方"。
         if (living.getNavigation().isInProgress()) {
            living.getNavigation().stop();
         }

         // 移动必须每 tick 执行：怪物的 AI 也是每 tick 跑的，
         // 如果只在"决策间隔"（约 2 秒）里走一步，位置会纹丝不动，
         // 而且会被它自己的漫游/漂浮行为立刻抵消。
         stepTowardTarget(living);

         if (returnHomeIfFar(living)) {
            return;
         }

         int interval = workInterval(living, job);
         if (level.getGameTime() % interval != Math.floorMod(living.getId(), interval)) {
            return;
         }

         if (recoverIfIdle(level, living)) {
            return;
         }

         runJob(level, living, job);

         flushBackpack(level, living, WorkerData.homeOf(living) != null ? WorkerData.homeOf(living) : living.blockPosition());
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[Worker] 工人任务异常，已跳过", t);
      }
   }

   /**
    * 每 tick 朝待办目标迈一小步。
    *
    * <p>用 {@code move(MoverType.SELF, ...)} 做带碰撞的位移：
    * 实测对这个实体，写 {@code deltaMovement} 与调用原版寻路都不会真正推动它，
    * 只有直接位移有效。
    */
   static void stepTowardTarget(LivingEntity worker) {
      int id = worker.getId();
      BlockPos target = MOVE_TARGET.get(id);
      if (target == null) {
         STALL_POS.remove(id);
         STALL_COUNT.remove(id);
         return;
      }

      if (canOperateAt(worker, target)) {
         MOVE_TARGET.remove(id);
         STALL_POS.remove(id);
         STALL_COUNT.remove(id);
         return;
      }

      // 停滞检测：位置连续多个 tick 不变，说明这个目标走不到（被墙挡住、悬空等）。
      // 此时把它记入黑名单并放弃，下一轮就会换别的目标 ——
      // 否则每轮都选同一块最近的方块，工人就会反复往同一个地方撞。
      BlockPos nowPos = worker.blockPosition();
      BlockPos prevPos = STALL_POS.put(id, nowPos);
      int stalled = prevPos != null && prevPos.equals(nowPos) ? STALL_COUNT.merge(id, 1, Integer::sum) : 0;
      if (stalled == 0) {
         STALL_COUNT.put(id, 0);
      }

      if (stalled >= STALL_TICKS) {
         markBlocked(target);
         MOVE_TARGET.remove(id);
         STALL_COUNT.put(id, 0);
         STALL_POS.remove(id);
         if (JiangshiDao.DIAG) {
            JiangshiDao.LOGGER.info("[工人] 目标走不到，已跳过 target={} worker={}", target, nowPos);
         }

         return;
      }

      double dx = target.getX() + 0.5 - worker.getX();
      double dz = target.getZ() + 0.5 - worker.getZ();
      double len = Math.sqrt(dx * dx + dz * dz);
      if (len < 0.001) {
         MOVE_TARGET.remove(id);
         return;
      }

      double step = Math.min(STEP_PER_TICK, len);

      // 直接设定位置，绕过实体自己的物理管线。
      // 原因：僵尸类用 tryHop() 沿自身朝向施加跳跃冲量、并用 travel(Vec3.ZERO)
      // 覆盖常规移动，任何"速度/寻路"层面的写入都会被它抵消，表现出来就是
      // "明明想靠近耕地，却被不断推向远处的某个方向"。
      double nx = worker.getX() + dx / len * step;
      double nz = worker.getZ() + dz / len * step;

      double dy = (target.getY() + 0.5) - (worker.getY() + 0.5);
      double ny = worker.getY();
      if (dy > 0.6) {
         ny = worker.getY() + 0.2;          // 目标更高：抬起
      } else if (dy < -1.0) {
         ny = worker.getY() - 0.15;         // 目标更低：下沉
      }

      worker.setPos(nx, ny, nz);
      worker.setDeltaMovement(0.0, 0.0, 0.0);
      worker.fallDistance = 0.0;

      float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      worker.setYRot(yaw);
      worker.yBodyRot = yaw;
      worker.yHeadRot = yaw;
   }

   /**
    * 闲时回归：长时间没有收成，就把工人领回铜铃旁重新开始找活。
    *
    * <p>判定依据是背包内容有没有变化（是否真的收到了东西），
    * 而不是"有没有调用工作方法" —— 它可能一直在绕圈但什么都没做成。
    *
    * @return true 表示本 tick 已处理，调用方应跳过本轮工作
    */
   static boolean recoverIfIdle(ServerLevel level, LivingEntity worker) {
      long now = level.getGameTime();
      int id = worker.getId();
      int signature = backpackSignature(worker, level);

      Integer last = LAST_WORK_SIGNATURE.get(id);
      if (last == null || last != signature) {
         LAST_WORK_SIGNATURE.put(id, signature);
         LAST_WORK_AT.put(id, now);
         return false;
      }

      long lastAt = LAST_WORK_AT.getOrDefault(id, now);
      if (now - lastAt < IDLE_TIMEOUT_TICKS) {
         return false;
      }

      LAST_WORK_AT.put(id, now);
      BlockPos home = WorkerData.homeOf(worker);
      if (home != null) {
         BlockPos spot = home.above();
         if (!level.getBlockState(spot).isAir() && !level.getBlockState(spot).canBeReplaced()) {
            spot = home;
         }

         worker.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
         worker.setDeltaMovement(0.0, 0.0, 0.0);
         worker.fallDistance = 0.0;
      }

      if (worker instanceof Mob mob) {
         mob.getNavigation().stop();
      }

      if (JiangshiDao.DIAG) {
         JiangshiDao.LOGGER.info("[工人] 闲时回归：{} 秒无收成，已回到铜铃旁", IDLE_TIMEOUT_TICKS / 20);
      }

      return true;
   }

   /**
    * 背包内容的轻量签名，用于判断是否真的收到了东西。
    */
   private static int backpackSignature(LivingEntity worker, ServerLevel level) {
      WorkerBackpack backpack = backpackOf(worker, level.registryAccess());
      int hash = 1;
      for (int i = 0; i < backpack.getContainerSize(); i++) {
         ItemStack stack = backpack.getItem(i);
         if (!stack.isEmpty()) {
            hash = hash * 31 + stack.getItem().hashCode();
            hash = hash * 31 + stack.getCount();
         }
      }

      return hash;
   }

   static void hint(LivingEntity worker, String message) {
      sendHint(worker, Component.literal("§7[僵尸道] §e" + message));
   }

   /** 走语言文件的提示（禁止硬编码文案，新代码请用这个重载）。 */
   static void hintKey(LivingEntity worker, String translationKey, Object... args) {
      sendHint(worker, Component.literal("§7[僵尸道] §e").append(Component.translatable(translationKey, args)));
   }

   private static void sendHint(LivingEntity worker, Component message) {
      if (worker.level() instanceof ServerLevel server) {
         long now = server.getGameTime();
         Long last = HINT_AT.get(worker.getId());
         if (last == null || now - last >= 60L) {
            HINT_AT.put(worker.getId(), now);

            for (ServerPlayer player : server.getPlayers(p -> p.distanceToSqr(worker) <= 2304.0)) {
               player.sendOverlayMessage(message);
            }
         }
      }
   }

   static void runJob(ServerLevel level, LivingEntity living, WorkerJob job) {
      switch (job) {
         case FARMING:
            farm(level, living);
            break;
         case FISHING:
            fish(level, living);
            break;
         case MINING:
            mine(level, living);
            break;
         case WOODCUTTING:
            chopWood(level, living);
            break;
         case PLANTING:
            plantSapling(level, living);
      }
   }

   /**
    * 判定工人是否已经站到可以操作该方块的位置（恢复原始阈值）。
    *
    * <p>水平 2.5 格、垂直 2.5 格。这是原始版本能正常工作时的值，
    * 放宽到 4 格反而会让"没走到位就开始干活"或反复犹豫。
    */
   static boolean canOperateAt(LivingEntity worker, BlockPos pos) {
      double dx = worker.getX() - (pos.getX() + 0.5);
      double dz = worker.getZ() - (pos.getZ() + 0.5);
      double dy = worker.getY() - pos.getY();
      // 用常量而不是硬编码：之前这里写着 6.25/2.5，而 OPERATE_RANGE 定义是 4.0，
      // 两者不一致，导致"以为放宽到 4 格、实际仍是 2.5 格"。
      return dx * dx + dz * dz <= OPERATE_RANGE * OPERATE_RANGE && Math.abs(dy) <= OPERATE_RANGE;
   }

   /**
    * 让工人走向目标点。
    *
    * <p>只登记"要去哪里"，真正的位移由 {@link #stepTowardTarget} 每 tick 执行。
    * 这样移动频率与怪物 AI 同步，不会被它的漫游行为覆盖。
    *
    * @param pos 目标方块位置
    */
   public static void walkTo(LivingEntity worker, BlockPos pos) {
      MOVE_TARGET.put(worker.getId(), pos.immutable());
   }

   /**
    * 工人（被铜铃绑定的怪物）只在铜铃的**水平 20 格**范围内活动，不跟随玩家。
    *
    * <p>判定用水平距离而非三维距离：垂直方向不可能跑出圈，且矿井/地表落差不应被误判为越界。
    * 越界时直接拉回铃铛旁，并清掉寻路与目标，避免它继续朝玩家或远处跑。
    *
    * @return 是否发生了"拉回"（调用方据此跳过本 tick 的工作）
    */
   static boolean returnHomeIfFar(LivingEntity worker) {
      BlockPos home = WorkerData.homeOf(worker);
      if (home == null) {
         return false;
      }

      double dx = worker.getX() - (home.getX() + 0.5);
      double dz = worker.getZ() - (home.getZ() + 0.5);
      if (dx * dx + dz * dz <= HOME_RADIUS_SQR) {
         return false;
      }

      if (worker.level() instanceof ServerLevel level) {
         BlockPos spot = home.above();
         if (!level.getBlockState(spot).isAir() && !level.getBlockState(spot).canBeReplaced()) {
            spot = home;
         }

         worker.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
         worker.setDeltaMovement(0.0, 0.0, 0.0);
         worker.fallDistance = 0.0;

         // 拉回后必须断掉寻路与仇恨，否则下一 tick 它又会朝原方向走。
         if (worker instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
         }

         hint(worker, "离铜铃超过 " + (int)HOME_RADIUS + " 格，已自动回到铃铛旁");
         return true;
      } else {
         return false;
      }
   }

   static int workInterval(LivingEntity worker, WorkerJob job) {
      double multiplier = TalentSet.of(worker).workSpeedMultiplier(job);
      int interval = (int)Math.round(40.0 / Math.max(0.3, multiplier));
      return Math.max(6, interval);
   }

   static boolean canFarmAt(LivingEntity worker, BlockPos pos) {
      int farming = TalentSet.of(worker).level(Talent.FARMING);
      return farming >= 4 ? worker.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0 : canOperateAt(worker, pos);
   }

   public static WorkerBackpack backpackOf(LivingEntity worker, Provider registries) {
      return BACKPACKS.computeIfAbsent(worker, w -> new WorkerBackpack(w, registries));
   }
}