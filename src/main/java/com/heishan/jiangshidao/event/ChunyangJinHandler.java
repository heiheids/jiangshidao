package com.heishan.jiangshidao.event;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.accessory.Accessories;
import com.heishan.jiangshidao.accessory.AccessorySlot;
import com.heishan.jiangshidao.item.ChunyangJinItem;
import com.heishan.jiangshidao.menu.AccessoryMenu;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 纯阳巾的服务端逻辑。
 *
 * <p>负责两件事：
 * <ol>
 *   <li><b>累计佩戴计时</b>：戴着道冠时每 tick 累加，每 {@link #FLUSH_INTERVAL} tick
 *       把进度写回物品的数据组件。不每 tick 写是因为组件会走网络同步，
 *       逐 tick 写会造成大量无意义的同步包。</li>
 *   <li><b>亡灵不主动攻击</b>：佩戴已激活道冠的玩家，不会被原版亡灵主动锁定；
 *       玩家先动手则照常还击。</li>
 * </ol>
 *
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ChunyangJinHandler {
   /** 写回物品组件的间隔（tick）：5 秒一次。 */
   private static final int FLUSH_INTERVAL = 100;
   /** 亡灵抑制的检查间隔（tick）。 */
   private static final int PEACE_INTERVAL = 10;
   /** 内存里的累计进度：玩家 UUID → 已累计 tick。 */
   private static final Map<UUID, Integer> WEARING = new ConcurrentHashMap<>();

   private ChunyangJinHandler() {
   }

   public static void register() {
      ServerTickEvents.END_SERVER_TICK.register(ChunyangJinHandler::onServerTick);
      JiangshiDao.LOGGER.info("[纯阳巾] 计时与亡灵抑制已注册");
   }

   private static void onServerTick(MinecraftServer server) {
      long time = server.overworld() == null ? 0L : server.overworld().getGameTime();
      boolean flushNow = time % FLUSH_INTERVAL == 0;
      boolean peaceNow = time % PEACE_INTERVAL == 0;

      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         tickWear(player, flushNow);

         if (peaceNow) {
            clearExistingTargets(player);
         }
      }
   }

   /**
    * 清掉"已经锁定在佩戴者身上"的亡灵目标。
    *
    * <p>新锁定由 {@code MobTargetSuppressMixin} 在 {@code setTarget} 处拦下，
    * 这里只处理"戴上道冠之前就已经锁定"的存量目标，所以范围限定在玩家附近，
    * 不做全世界实体遍历。
    */
   private static void clearExistingTargets(ServerPlayer player) {
      if (!ChunyangJinItem.isWearingActive(player)) {
         return;
      }

      ServerLevel level = player.level();
      for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64.0))) {
         if (!mob.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)) {
            continue;
         }

         if (mob.getTarget() == player && mob.getLastHurtByMob() != player) {
            mob.setTarget(null);
         }
      }
   }

   /**
    * 推进单个玩家的累计佩戴时长。
    *
    * @param flush 是否到了写回物品的时机
    */
   private static void tickWear(ServerPlayer player, boolean flush) {
      // 非落盘 tick 完全不碰饰品库存：
      // AccessoryStorage.get() 内部会 setDirty()，每 tick 调用会让存档一直处于脏状态，
      // 触发远超必要的写盘。这里只累加内存计数，佩戴状态在落盘 tick 判定。
      if (!flush) {
         WEARING.merge(player.getUUID(), 1, Integer::sum);
         return;
      }

      ItemStack head = Accessories.of(player).get(AccessorySlot.HEAD);
      if (!(head.getItem() instanceof ChunyangJinItem)) {
         // 没戴：清掉残留计数，避免把未佩戴的时间也算进去
         WEARING.remove(player.getUUID());
         return;
      }

      int total = WEARING.merge(player.getUUID(), 1, Integer::sum);
      WEARING.put(player.getUUID(), 0);
      ChunyangJinItem.setWearTicks(head, ChunyangJinItem.wearTicks(head) + total);

      // 改完必须触发容器变更：客户端看到的是同步副本，
      // 只有经过 changeListener → slotsChanged → Accessories.sync 才会刷新，
      // 否则 tooltip 会一直停在旧数值（表现为"计时不动"）。
      jiangshidao$markDirty(player);

      if (JiangshiDao.DIAG) {
         JiangshiDao.LOGGER.info(
            "[CHUNYANG] wear={} active={} player={}",
            new Object[]{ChunyangJinItem.wearTicks(head), ChunyangJinItem.isActive(head), player.getName().getString()}
         );
      }
   }

   /**
    * 通知饰品容器内容已变化，让同步包发出去。
    */
   private static void jiangshidao$markDirty(ServerPlayer player) {
      try {
         if (player.containerMenu instanceof AccessoryMenu menu) {
            menu.slotsChanged(menu.accessoryContainer());
         } else {
            // 没开界面时也要同步：直接推一次完整槽位列表给客户端，
            // 供头顶渲染与女鬼显形读取。
            Accessories.sync(player);
         }
      } catch (Throwable ignored) {
         // 同步失败不应影响计时本身
      }
   }

}