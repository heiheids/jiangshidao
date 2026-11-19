package com.heishan.jiangshidao.event;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.block.LightningLogBlock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.Load;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;

/**
 * LightningIgniteHandler —— 事件/任务处理。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class LightningIgniteHandler {
   private LightningIgniteHandler() {
   }

   public static void register() {
      ServerEntityEvents.ENTITY_LOAD.register((Load)(entity, level) -> {
         if (entity instanceof LightningBolt bolt) {
            try {
               LightningLogBlock.igniteNearby(level, bolt.blockPosition());
            } catch (Throwable t) {
               JiangshiDao.LOGGER.error("[落雷] 引燃桃木失败", t);
            }
         }
      });
   }

   public static void onLightningJoin(ServerLevel level, LightningBolt bolt) {
      LightningLogBlock.igniteNearby(level, bolt.blockPosition());
   }
}
