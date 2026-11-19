package com.heishan.jiangshidao.event;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.accessory.AccessoryInventory;
import com.heishan.jiangshidao.accessory.AccessorySlot;
import com.heishan.jiangshidao.accessory.AccessoryStorage;
import com.heishan.jiangshidao.item.BronzeBellItem;
import com.heishan.jiangshidao.registry.ModEffects;
import com.heishan.jiangshidao.worker.BellMode;
import com.heishan.jiangshidao.worker.Talent;
import com.heishan.jiangshidao.worker.TalentSet;
import com.heishan.jiangshidao.worker.WorkerData;
import com.heishan.jiangshidao.worker.WorkerHandler;
import com.heishan.jiangshidao.worker.WorkerJob;
import com.heishan.jiangshidao.worker.WorkerPanelService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * WarCombatHandler —— 事件/任务处理。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class WarCombatHandler {
   private static final double FOLLOW_START = 6.0;
   private static final double ENEMY_RANGE = 12.0;
   private static final double HIT_RANGE = 2.6;
   private static final int ATTACK_COOLDOWN = 20;
   private static final Map<Integer, Long> NEXT_HIT = new ConcurrentHashMap<>();
   private static final Map<UUID, long[]> PROVOKED = new ConcurrentHashMap<>();
   private static final long PROVOKE_TICKS = 200L;

   private WarCombatHandler() {
   }

   public static void register() {
      ServerTickEvents.END_SERVER_TICK.register(WarCombatHandler::tickServer);
      ServerLivingEntityEvents.ALLOW_DAMAGE.register(WarCombatHandler::onOwnerHurt);
   }

   private static boolean onOwnerHurt(LivingEntity entity, DamageSource source, float amount) {
      if (entity instanceof ServerPlayer player) {
         if (source.getEntity() instanceof Mob attacker) {
            PROVOKED.put(player.getUUID(), new long[]{attacker.getId(), player.level().getGameTime() + 200L});
            return true;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static void tickServer(MinecraftServer server) {
      for (ServerLevel level : server.getAllLevels()) {
         try {
            tickLevel(level);
         } catch (Throwable t) {
            JiangshiDao.LOGGER.error("[战意] tick 异常，已跳过该维度", t);
         }
      }
   }

   private static void tickLevel(ServerLevel level) {
      for (Entity entity : level.getAllEntities()) {
         if (entity instanceof Mob mob) {
            try {
               tickMob(level, mob);
            } catch (Throwable t) {
               JiangshiDao.LOGGER.error("[WarCombat] 战意工人逻辑异常，已跳过", t);
            }
         }
      }
   }

   private static void tickMob(ServerLevel level, Mob mob) {
      if (!ModEffects.isSubdued(mob)) {
         NEXT_HIT.remove(mob.getId());
      } else if (TalentSet.of(mob).has(Talent.WAR)) {
         ServerPlayer bellOwner = bellOwnerOf(mob);
         if (bellOwner != null) {
            BellMode mode = WorkerPanelService.bellModeOf(bellOwner);
            if (mode != BellMode.DISABLED) {
               if (WorkerData.jobOf(mob) == WorkerJob.IDLE) {
                  tickCombat(level, mob, mode);
               }
            }
         }
      }
   }

   private static void tickCombat(ServerLevel level, Mob mob, BellMode mode) {
      boolean mayAttack = mode == BellMode.AGGRESSIVE && WorkerData.attackEnabled(mob);
      if (mode == BellMode.PASSIVE && WorkerData.attackEnabled(mob)) {
         Mob avenger = findProvoked(level, mob);
         if (avenger != null) {
            mob.getLookControl().setLookAt(avenger, 30.0F, 30.0F);
            if (mob.distanceToSqr(avenger) <= 6.760000000000001) {
               tryAttack(level, mob, avenger);
            } else {
               WorkerHandler.walkTo(mob, avenger.blockPosition());
            }

            return;
         }

         mayAttack = false;
      }

      if (mayAttack) {
         Mob enemy = findEnemy(level, mob);
         if (enemy != null) {
            mob.getLookControl().setLookAt(enemy, 30.0F, 30.0F);
            if (mob.distanceToSqr(enemy) <= 6.760000000000001) {
               tryAttack(level, mob, enemy);
            } else {
               WorkerHandler.walkTo(mob, enemy.blockPosition());
            }

            return;
         }
      }

      if (WorkerData.followEnabled(mob)) {
         Player owner = findOwner(level, mob);
         if (owner != null && owner.isAlive()) {
            if (mob.distanceToSqr(owner) > 36.0) {
               WorkerHandler.walkTo(mob, owner.blockPosition());
            }
         }
      }
   }

   private static void tryAttack(ServerLevel level, Mob mob, Mob enemy) {
      long now = level.getGameTime();
      Long next = NEXT_HIT.get(mob.getId());
      if (next == null || now >= next) {
         NEXT_HIT.put(mob.getId(), now + 20L);
         mob.doHurtTarget(level, enemy);
      }
   }

   private static ServerPlayer bellOwnerOf(Mob mob) {
      MinecraftServer server = mob.level().getServer();
      if (server == null) {
         return null;
      }

      UUID ownerId = WorkerData.ownerOf(mob);
      ServerPlayer owner = ownerId == null ? null : server.getPlayerList().getPlayer(ownerId);
      if (owner == null && mob.level().getNearestPlayer(mob, 16.0) instanceof ServerPlayer nearest) {
         owner = nearest;
      }

      if (owner == null) {
         return null;
      }

      AccessoryInventory accessories = AccessoryStorage.get(server).peek(owner.getUUID());
      if (accessories == null) {
         return null;
      }

      boolean hasBell = accessories.get(AccessorySlot.LEGS).getItem() instanceof BronzeBellItem;
      return hasBell ? owner : null;
   }

   private static Mob findProvoked(ServerLevel level, Mob mob) {
      Player owner = findOwner(level, mob);
      if (owner == null) {
         return null;
      } else {
         long[] record = PROVOKED.get(owner.getUUID());
         if (record == null) {
            return null;
         } else if (level.getGameTime() > record[1]) {
            PROVOKED.remove(owner.getUUID());
            return null;
         } else if (level.getEntity((int)record[0]) instanceof Mob attacker && attacker.isAlive()) {
            return attacker;
         } else {
            PROVOKED.remove(owner.getUUID());
            return null;
         }
      }
   }

   private static Mob findEnemy(ServerLevel level, Mob mob) {
      Mob best = null;
      double bestDist = Double.MAX_VALUE;

      for (Mob other : level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(12.0))) {
         if (other != mob && other.isAlive() && other instanceof Enemy && !ModEffects.isSubdued(other) && other.getType() != mob.getType()) {
            double dist = mob.distanceToSqr(other);
            if (dist < bestDist) {
               bestDist = dist;
               best = other;
            }
         }
      }

      return best;
   }

   private static Player findOwner(ServerLevel level, Mob mob) {
      UUID ownerId = WorkerData.ownerOf(mob);
      if (ownerId != null) {
         Player owner = level.getServer().getPlayerList().getPlayer(ownerId);
         if (owner != null && owner.level() == level) {
            return owner;
         }
      }

      return level.getNearestPlayer(mob, 16.0);
   }
}
