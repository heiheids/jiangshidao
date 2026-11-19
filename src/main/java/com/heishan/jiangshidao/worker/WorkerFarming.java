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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

// 拆分后的跨类静态导入：方法体保持原样，裸调用在此解析。
import static com.heishan.jiangshidao.worker.WorkerHandler.isBlocked;
import static com.heishan.jiangshidao.worker.WorkerHandler.*;
import static com.heishan.jiangshidao.worker.WorkerFarming.*;
import static com.heishan.jiangshidao.worker.WorkerFishing.*;
import static com.heishan.jiangshidao.worker.WorkerWoodcutting.*;
import static com.heishan.jiangshidao.worker.WorkerMining.*;
import static com.heishan.jiangshidao.worker.WorkerTools.*;
import static com.heishan.jiangshidao.worker.WorkerStorage.*;

/**
 * 工人任务：种植、催熟与作物收割。
 * 由 {@link WorkerHandler} 调度，方法体自 WorkerHandler 原样拆出。
 */
final class WorkerFarming {
   /** 工人能种的作物方块。新增作物时只需在这里补充。 */
   private static final Block[] CROP_BLOCKS = new Block[]{
      Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS
   };

   /** 上一轮所在位置，用于判断"是否卡住走不动"。 */
   private static final Map<Integer, BlockPos> LAST_POS = new ConcurrentHashMap<>();
   /** 连续卡住的轮数。 */
   private static final Map<Integer, Integer> STUCK_COUNT = new ConcurrentHashMap<>();
   /** 够不着的作物/耕地冷却表：坐标 → 冷却结束的 gameTime。 */
   private static final Map<BlockPos, Long> CROP_COOLDOWN = new ConcurrentHashMap<>();
   /** 不可达目标的冷却时长（tick）。 */
   private static final long CROP_COOLDOWN_TICKS = 200L;

   private WorkerFarming() {
   }

   /**
    * 朝目标移动；若连续多轮位置毫无变化（被地形/方块挡住），直接挪到目标旁。
    *
    * <p>工人是靠直接位移移动的，遇到台阶、墙、耕地高差时可能被卡死，
    * 表现为"原地不动却一直想走过去"。这里加兜底，保证它最终能到达活儿点。
    */
   private static void approach(LivingEntity worker, BlockPos target) {
      // 只登记目标；位移由 WorkerHandler.stepTowardTarget 每 tick 执行。
      walkTo(worker, target);
   }

   /**
    * 干活中心的坐标：用工人当前位置。
    *
    * <p>以自身为中心搜索，才能"走到哪、就近干到哪"，不会追着远处的目标跑。
    */
   static BlockPos centerOf(LivingEntity worker) {
      return worker.blockPosition();
   }

   static void farm(ServerLevel level, LivingEntity worker) {
      BlockPos origin = worker.blockPosition();
      boostCropGrowth(level, worker, origin);

      // 挑最近的一块成熟作物，而不是扫描顺序上的第一块。
      BlockPos best = null;
      double bestDist = Double.MAX_VALUE;
      int ripe = 0;
      int young = 0;
      int emptyFarmland = 0;

      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-WORK_RADIUS, -3, -WORK_RADIUS), origin.offset(WORK_RADIUS, 3, WORK_RADIUS))) {
         BlockState state = level.getBlockState(pos);
         if (state.is(Blocks.FARMLAND)) {
            if (level.getBlockState(pos.above()).isAir()) {
               emptyFarmland++;
            }

            continue;
         }

         if (state.getBlock() instanceof CropBlock crop) {
            if (crop.isMaxAge(state)) {
               if (isBlocked(pos)) {
                  continue;
               }

               if (isCoolingDown(level, pos)) {
                  continue;   // 这块刚被判定够不着，冷却期内跳过
               }

               ripe++;
               double d = pos.distSqr(origin);

               // 已经在前往这块成熟作物时，优先保持，避免目标每轮跳动。
               boolean onTheWay = pos.immutable().equals(WorkerHandler.currentMoveTarget(worker));
               if (onTheWay || d < bestDist) {
                  if (onTheWay) {
                     bestDist = d;
                  } else {
                     bestDist = d;
                  }

                  best = pos.immutable();
               }
            } else {
               young++;
            }
         }
      }

      if (JiangshiDao.DIAG) {
         JiangshiDao.LOGGER.info("[耕种诊断] 成熟={} 幼苗={} 空耕地={} 中心={} 目标={}",
            ripe, young, emptyFarmland, origin, best);
      }

      if (best != null) {
         if (!canOperateAt(worker, best)) {
            approach(worker, best);
            return;
         }

         BlockState state = level.getBlockState(best);
         List<ItemStack> drops = Block.getDrops(state, level, best, null);
         if (state.getBlock() instanceof CropBlock crop) {
            level.setBlockAndUpdate(best, crop.getStateForAge(0));
         }

         deposit(level, worker, origin, drops);
         return;
      }

      plantCrop(level, worker);
   }

   static void plantCrop(ServerLevel level, LivingEntity worker) {
      BlockPos origin = worker.blockPosition();
      BlockPos target = null;
      double bestDist = Double.MAX_VALUE;

      // 优先沿用"已经在前往"的那块地。
      // 否则每轮都按当前距离重新挑最近的空耕地，工人一移动目标就换，
      // 会在两块地之间来回摆动、永远走不到。
      BlockPos existing = WorkerHandler.currentMoveTarget(worker);
      if (existing != null
         && level.getBlockState(existing).is(Blocks.FARMLAND)
         && level.getBlockState(existing.above()).isAir()
         && !isBlocked(existing)) {
         target = existing;
      }

      // 没有在途目标时才重新挑选。
      if (target == null) {
         for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-WORK_RADIUS, -3, -WORK_RADIUS), origin.offset(WORK_RADIUS, 3, WORK_RADIUS))) {
            if (level.getBlockState(pos).is(Blocks.FARMLAND) && level.getBlockState(pos.above()).isAir()) {
               if (isBlocked(pos)) {
                  continue;
               }

               if (isCoolingDown(level, pos)) {
                  continue;   // 这块刚被判定够不着，冷却期内跳过
               }

               double d = pos.distSqr(origin);
               if (d < bestDist) {
                  bestDist = d;
                  target = pos.immutable();
               }
            }
         }
      }

      if (target != null) {
         // target 是耕地本身，作物要种在它上面一格。
         BlockPos plantAt = target.above();

         if (!canOperateAt(worker, target)) {
            approach(worker, target);
         } else {
            // 先用自己的背包播种，再找附近的箱子；两处都没有种子才算无计可施。
            if (plantFrom(worker, WorkerHandler.backpackOf(worker, level.registryAccess()), plantAt, level)) {
               return;
            }

            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-WORK_RADIUS, -3, -WORK_RADIUS), origin.offset(WORK_RADIUS, 3, WORK_RADIUS))) {
               if (level.getBlockEntity(pos) instanceof Container container) {
                  if (plantFrom(worker, container, plantAt, level)) {
                     return;
                  }
               }
            }

            // 没有种子就别装作在干活：给出提示，让玩家知道该往背包/箱子里补种子。
            hintKey(worker, "message.jiangshidao.worker.no_seeds");
            if (JiangshiDao.DIAG) {
               JiangshiDao.LOGGER.info("[播种诊断] 找到空耕地但没种成：背包={} 附近箱子数={}",
                  countSeed(WorkerHandler.backpackOf(worker, level.registryAccess())), countNearbyContainers(level, origin));
            }
         }
      } else {
         // 周围既没有成熟作物、也没有空耕地：回铃边待命，别杵在远处不动。
         if (worker.distanceToSqr(origin.getX() + 0.5, worker.getY(), origin.getZ() + 0.5) > 9.0) {
            approach(worker, origin.above());
         }
      }
   }

   /**
    * 从指定容器里取一份作物种子种到 {@code target}。
    *
    * @return 是否成功播种
    */
   private static boolean plantFrom(LivingEntity worker, Container container, BlockPos plantAt, ServerLevel level) {
      if (container == null) {
         return false;
      }

      for (int i = 0; i < container.getContainerSize(); i++) {
         ItemStack stack = container.getItem(i);
         Block block = cropFor(stack);
         if (block == null) {
            continue;
         }

         level.setBlockAndUpdate(plantAt, block.defaultBlockState());
         stack.shrink(1);
         container.setChanged();
         return true;
      }

      return false;
   }

   /**
    * 统计容器里能被识别为种子的物品总数量（诊断用）。
    */
   private static int countSeed(Container container) {
      if (container == null) {
         return -1;
      }

      int total = 0;
      for (int i = 0; i < container.getContainerSize(); i++) {
         ItemStack stack = container.getItem(i);
         if (cropFor(stack) != null) {
            total += stack.getCount();
         }
      }

      return total;
   }

   /**
    * 统计以中心为原点、工作半径内的容器（箱子等）数量（诊断用）。
    */
   private static int countNearbyContainers(ServerLevel level, BlockPos origin) {
      int count = 0;
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-WORK_RADIUS, -3, -WORK_RADIUS), origin.offset(WORK_RADIUS, 3, WORK_RADIUS))) {
         if (level.getBlockEntity(pos) instanceof Container) {
            count++;
         }
      }

      return count;
   }

   /**
    * 把这次够不着的作物/耕地标记为冷却，一段时间内不再选它。
    */
   private static void markCropUnreachable(Level level, BlockPos pos) {
      long now = level instanceof net.minecraft.server.level.ServerLevel server ? server.getGameTime() : 0L;
      CROP_COOLDOWN.put(pos.immutable(), now + CROP_COOLDOWN_TICKS);
   }

   /**
    * 该坐标是否还在冷却中。
    */
   private static boolean isCoolingDown(Level level, BlockPos pos) {
      Long until = CROP_COOLDOWN.get(pos);
      if (until == null) {
         return false;
      }

      long now = level instanceof net.minecraft.server.level.ServerLevel server ? server.getGameTime() : 0L;
      if (until <= now) {
         CROP_COOLDOWN.remove(pos);
         return false;
      }

      return true;
   }

   /**
    * 这个物品能种出哪种作物；不是种子则返回 null。
    *
    * <p>不能用 {@code instanceof BlockItem} 判断：26.x 里小麦种子、胡萝卜、马铃薯
    * 都是普通 {@code Item}，`ItemNameBlockItem` 已不存在，那样判断会永远为假，
    * 表现就是"有种子、有空耕地，却一直空闲"。
    *
    * <p>也不能用 {@code CropBlock.getBaseSeedId()}：它是 protected，外部无法调用。
    * 因此改为按物品反查：该物品对应的方块若是作物，就用它。
    */
   private static Block cropFor(ItemStack stack) {
      if (stack.isEmpty()) {
         return null;
      }

      for (Block candidate : CROP_BLOCKS) {
         if (itemFor(candidate) == stack.getItem()) {
            return candidate;
         }
      }

      return null;
   }

   /**
    * 取作物方块对应的种子物品。
    *
    * <p>小麦 / 胡萝卜 / 马铃薯 / 甜菜的种子物品各自不同，这里显式映射，
    * 避免依赖 protected 或内部 API。
    */
   private static Item itemFor(Block cropBlock) {
      if (cropBlock == Blocks.WHEAT) {
         return Items.WHEAT_SEEDS;
      } else if (cropBlock == Blocks.CARROTS) {
         return Items.CARROT;
      } else if (cropBlock == Blocks.POTATOES) {
         return Items.POTATO;
      } else if (cropBlock == Blocks.BEETROOTS) {
         return Items.BEETROOT_SEEDS;
      } else {
         return null;
      }
   }

   static void boostCropGrowth(ServerLevel level, LivingEntity worker, BlockPos origin) {
      int farming = TalentSet.of(worker).level(Talent.FARMING);
      if (farming >= 5) {
         int attempts = 0;

         for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-WORK_RADIUS, -3, -WORK_RADIUS), origin.offset(WORK_RADIUS, 3, WORK_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state) && !(level.getRandom().nextFloat() > 0.35F)) {
               state.randomTick(level, pos.immutable(), level.getRandom());
               if (++attempts >= 3) {
                  return;
               }
            }
         }
      }
   }

   static void boostSaplings(ServerLevel level, LivingEntity worker, BlockPos origin) {
      int woodcutting = TalentSet.of(worker).level(Talent.WOODCUTTING);
      if (woodcutting >= 5) {
         for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-WORK_RADIUS, -3, -WORK_RADIUS), origin.offset(WORK_RADIUS, 3, WORK_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof SaplingBlock) {
               for (int i = 0; i < 4; i++) {
                  state.randomTick(level, pos.immutable(), level.getRandom());
               }
            }
         }
      }
   }

   static void plantSapling(ServerLevel level, LivingEntity worker) {
      BlockPos origin = worker.blockPosition();
      BlockPos target = null;

      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-WORK_RADIUS, -3, -WORK_RADIUS), origin.offset(WORK_RADIUS, 3, WORK_RADIUS))) {
         BlockState ground = level.getBlockState(pos);
         BlockPos above = pos.above();
         if ((ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT)) && level.getBlockState(above).isAir()) {
            target = above.immutable();
            break;
         }
      }

      if (target != null) {
         for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-WORK_RADIUS, -3, -WORK_RADIUS), origin.offset(WORK_RADIUS, 3, WORK_RADIUS))) {
            if (level.getBlockEntity(pos) instanceof Container container) {
               for (int i = 0; i < container.getContainerSize(); i++) {
                  ItemStack stack = container.getItem(i);
                  if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                     BlockState placed = blockItem.getBlock().defaultBlockState();
                     if (placed.is(BlockTags.SAPLINGS)) {
                        level.setBlockAndUpdate(target, placed);
                        stack.shrink(1);
                        container.setChanged();
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   static void plantSaplingsFromBackpack(ServerLevel level, LivingEntity worker, BlockPos origin) {
      WorkerBackpack backpack = backpackOf(worker, level.registryAccess());
      int slot = -1;
      ItemStack sapling = ItemStack.EMPTY;

      for (int i = 0; i < backpack.getContainerSize(); i++) {
         ItemStack stack = backpack.getItem(i);
         if (!stack.isEmpty() && stack.is(ItemTags.SAPLINGS)) {
            slot = i;
            sapling = stack;
            break;
         }
      }

      if (slot >= 0 && sapling.getItem() instanceof BlockItem blockItem) {
         for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-WORK_RADIUS, -3, -WORK_RADIUS), origin.offset(WORK_RADIUS, 3, WORK_RADIUS))) {
            if (isPlantableGround(level.getBlockState(pos))) {
               BlockPos above = pos.above();
               if (level.getBlockState(above).isAir()) {
                  BlockState plant = blockItem.getBlock().defaultBlockState();
                  if (plant.canSurvive(level, above)) {
                     level.setBlockAndUpdate(above.immutable(), plant);
                     sapling.shrink(1);
                     if (sapling.isEmpty()) {
                        backpack.setItem(slot, ItemStack.EMPTY);
                     }

                     backpack.setChanged();
                     return;
                  }
               }
            }
         }
      }
   }

   static boolean isPlantableGround(BlockState state) {
      return state.is(Blocks.GRASS_BLOCK)
         || state.is(Blocks.DIRT)
         || state.is(Blocks.PODZOL)
         || state.is(Blocks.COARSE_DIRT)
         || state.is(Blocks.ROOTED_DIRT)
         || state.is(Blocks.MOSS_BLOCK);
   }

   static BlockPos nearestBlock(ServerLevel level, LivingEntity worker, BlockPos origin, Block... blocks) {
      BlockPos best = null;
      double bestDist = Double.MAX_VALUE;

      // 搜索半径 20 格、垂直 ±8：覆盖整片矿洞，同时避免 41³ 全量扫描带来的开销。
      int vertical = 8;
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-WORK_RADIUS, -vertical, -WORK_RADIUS), origin.offset(WORK_RADIUS, vertical, WORK_RADIUS))) {
         BlockState state = level.getBlockState(pos);
         boolean match = false;

         for (Block block : blocks) {
            if (state.is(block)) {
               match = true;
               break;
            }
         }

         if (match && level.getBlockEntity(pos) == null) {
            double dist = worker.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (dist < bestDist) {
               bestDist = dist;
               best = pos.immutable();
            }
         }
      }

      return best;
   }
}