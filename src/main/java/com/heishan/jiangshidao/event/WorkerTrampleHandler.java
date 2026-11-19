package com.heishan.jiangshidao.event;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.worker.WorkerData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

/**
 * WorkerTrampleHandler —— 事件/任务处理。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class WorkerTrampleHandler {
   private WorkerTrampleHandler() {
   }

   public static void register() {
      ServerTickEvents.END_SERVER_TICK.register(WorkerTrampleHandler::tickServer);
   }

   public static boolean shouldProtect(Entity entity) {
      return entity == null ? false : WorkerData.isSubduedWorker(entity);
   }

   private static void tickServer(MinecraftServer server) {
      for (ServerLevel level : server.getAllLevels()) {
         try {
            tickLevel(level);
         } catch (Throwable t) {
            JiangshiDao.LOGGER.error("[耕地保护] tick 异常，已跳过该维度", t);
         }
      }
   }

   private static void tickLevel(ServerLevel level) {
      for (Entity entity : level.getAllEntities()) {
         if (entity instanceof LivingEntity living && shouldProtect(living)) {
            try {
               if (nearFarmland(level, living.blockPosition())) {
                  living.resetFallDistance();
               }
            } catch (Throwable t) {
               JiangshiDao.LOGGER.error("[耕地保护] 处理失败，已跳过该实体", t);
            }
         }
      }
   }

   private static boolean nearFarmland(ServerLevel level, BlockPos pos) {
      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            for (int dy = -1; dy <= 0; dy++) {
               if (level.getBlockState(pos.offset(dx, dy, dz)).is(Blocks.FARMLAND)) {
                  return true;
               }
            }
         }
      }

      return false;
   }
}
