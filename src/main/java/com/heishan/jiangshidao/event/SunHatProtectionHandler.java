package com.heishan.jiangshidao.event;

import com.heishan.jiangshidao.JiangshiDao;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * SunHatProtectionHandler —— 事件/任务处理。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class SunHatProtectionHandler {
   private SunHatProtectionHandler() {
   }

   public static void register() {
      ServerTickEvents.END_SERVER_TICK.register(SunHatProtectionHandler::tickServer);
   }

   private static void tickServer(MinecraftServer server) {
      for (ServerLevel level : server.getAllLevels()) {
         try {
            tickLevel(level);
         } catch (Throwable t) {
            JiangshiDao.LOGGER.error("[遮阳] tick 异常，已跳过该维度", t);
         }
      }
   }

   private static void tickLevel(ServerLevel level) {
      for (Entity entity : level.getAllEntities()) {
         if (entity instanceof LivingEntity living
            && living.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)
            && !living.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            try {
               if (living.getRemainingFireTicks() > 0 || living.isOnFire()) {
                  living.clearFire();
               }
            } catch (Throwable t) {
               JiangshiDao.LOGGER.error("[遮阳] 清火失败，已跳过该实体", t);
            }
         }
      }
   }
}
