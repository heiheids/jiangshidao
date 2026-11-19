package com.heishan.jiangshidao.event;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.accessory.Accessories;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents.AfterPlayerChange;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AfterRespawn;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * AccessorySyncHandler —— 事件/任务处理。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class AccessorySyncHandler {
   private AccessorySyncHandler() {
   }

   public static void register() {
      ServerPlayerEvents.JOIN.register(AccessorySyncHandler::scheduleSync);
      ServerPlayerEvents.AFTER_RESPAWN.register((AfterRespawn)(oldPlayer, newPlayer, alive) -> scheduleSync(newPlayer));
      ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((AfterPlayerChange)(player, origin, destination) -> scheduleSync(player));
   }

   private static void scheduleSync(ServerPlayer player) {
      if (player != null) {
         MinecraftServer server = player.level().getServer();
         if (server != null) {
            server.execute(() -> {
               if (!player.hasDisconnected()) {
                  try {
                     Accessories.sync(player);
                  } catch (Throwable t) {
                     JiangshiDao.LOGGER.error("[附加装备] 同步失败", t);
                  }
               }
            });
         }
      }
   }
}
