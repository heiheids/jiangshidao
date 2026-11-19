package com.heishan.jiangshidao.event;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.entity.GhostWomanEntity;
import com.heishan.jiangshidao.registry.ModEffects;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * SealedEffectHandler —— 事件/任务处理。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class SealedEffectHandler {
   private static final Map<LivingEntity, double[]> ANCHORS = Collections.synchronizedMap(new WeakHashMap<>());

   private SealedEffectHandler() {
   }

   public static void register() {
      ServerTickEvents.END_SERVER_TICK.register(SealedEffectHandler::tickServer);
   }

   private static void tickServer(MinecraftServer server) {
      for (ServerLevel level : server.getAllLevels()) {
         try {
            tickLevel(level);
         } catch (Throwable t) {
            JiangshiDao.LOGGER.error("[镇尸] tick 异常，已跳过该维度", t);
         }
      }
   }

   private static void tickLevel(ServerLevel level) {
      for (Entity entity : level.getAllEntities()) {
         if (entity instanceof LivingEntity living) {
            if (!ModEffects.isSealed(living)) {
               ANCHORS.remove(living);
            } else {
               try {
                  freeze(living);
               } catch (Throwable t) {
                  JiangshiDao.LOGGER.error("[镇尸] 冻结失败，已跳过该实体", t);
               }
            }
         }
      }
   }

   private static void freeze(LivingEntity entity) {
      double[] anchor = ANCHORS.get(entity);
      if (anchor == null) {
         anchor = new double[]{entity.getX(), entity.getZ()};
         ANCHORS.put(entity, anchor);
      }

      boolean grounded = entity.onGround();
      double y = entity.getY();
      entity.setPos(anchor[0], y, anchor[1]);
      if (grounded) {
         entity.setDeltaMovement(0.0, 0.0, 0.0);
      } else {
         Vec3 v = entity.getDeltaMovement();
         double vy = Math.min(v.y, 0.0);
         entity.setDeltaMovement(0.0, vy, 0.0);
      }

      if (entity instanceof Mob mob) {
         mob.getNavigation().stop();
         mob.setTarget(null);
         mob.setJumping(false);
      }

      if (entity instanceof GhostWomanEntity ghost && !grounded) {
         ghost.getMoveControl().setWantedPosition(entity.getX(), entity.getY() - 3.0, entity.getZ(), 0.5);
      }

      entity.setSprinting(false);
      entity.setShiftKeyDown(false);
   }
}
