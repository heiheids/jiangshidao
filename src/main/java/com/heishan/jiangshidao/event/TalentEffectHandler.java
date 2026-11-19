package com.heishan.jiangshidao.event;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.compat.PersistentData;
import com.heishan.jiangshidao.item.BronzeBellItem;
import com.heishan.jiangshidao.registry.ModDataComponents;
import com.heishan.jiangshidao.registry.ModEffects;
import com.heishan.jiangshidao.worker.Talent;
import com.heishan.jiangshidao.worker.TalentSet;
import com.heishan.jiangshidao.worker.WorkerData;
import com.heishan.jiangshidao.worker.WorkerJob;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.Load;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * TalentEffectHandler —— 事件/任务处理。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class TalentEffectHandler {
   public static final String REVIVE_DEADLINE = "JiangshiReviveDeadline";
   private static final long ONE_GAME_DAY = 24000L;
   public static final int WAR_NO_DEATH_LEVEL = 4;
   private static final double BELL_SEARCH_RADIUS = 64.0;

   private TalentEffectHandler() {
   }

   public static void register() {
      ServerEntityEvents.ENTITY_LOAD.register((Load)(entity, level) -> {
         if (entity instanceof LivingEntity living) {
            try {
               applyAttributeTalents(living);
            } catch (Throwable t) {
               JiangshiDao.LOGGER.error("[天赋] 属性应用失败", t);
            }
         }
      });
      ServerLivingEntityEvents.ALLOW_DAMAGE.register(TalentEffectHandler::onIncomingDamage);
      ServerLivingEntityEvents.AFTER_DAMAGE.register(TalentEffectHandler::afterDamage);
      ServerLivingEntityEvents.ALLOW_DEATH.register(TalentEffectHandler::allowDeath);
   }

   public static void applyAttributeTalents(LivingEntity living) {
      if (ModEffects.isSubdued(living)) {
         TalentSet set = TalentSet.of(living);
         if (!set.isEmpty()) {
            set.applyAttributeModifiers();
         }
      }
   }

   private static boolean onIncomingDamage(LivingEntity living, DamageSource source, float amount) {
      if (!ModEffects.isSubdued(living)) {
         return true;
      }

      if (living instanceof Mob mob) {
         if (TalentSet.of(mob).level(Talent.WAR) < 4) {
            return true;
         }

         if (amount < mob.getHealth()) {
            return true;
         }

         if (mob.level() instanceof ServerLevel level) {
            try {
               preventDeath(level, mob);
            } catch (Throwable t) {
               JiangshiDao.LOGGER.error("[战意] 致死拦截失败", t);
            }

            return false;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static void afterDamage(LivingEntity living, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
      if (!blocked && !(damageTaken <= 0.0F)) {
         if (living.isAlive() && ModEffects.isSubdued(living)) {
            double defense = TalentSet.of(living).defenseMultiplier();
            if (!(defense <= 1.0)) {
               float refund = (float)(damageTaken * (1.0 - 1.0 / defense));
               if (!(refund <= 0.0F)) {
                  living.setHealth(Math.min(living.getMaxHealth(), living.getHealth() + refund));
               }
            }
         }
      }
   }

   private static boolean allowDeath(LivingEntity living, DamageSource source, float amount) {
      if (ModEffects.isSubdued(living) && living instanceof Mob mob) {
         if (TalentSet.of(mob).level(Talent.WAR) < 4) {
            return true;
         }

         if (mob.level() instanceof ServerLevel level) {
            try {
               preventDeath(level, mob);
            } catch (Throwable t) {
               JiangshiDao.LOGGER.error("[战意] 死亡拦截失败", t);
            }

            return false;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static void preventDeath(ServerLevel level, Mob mob) {
      mob.setHealth(mob.getMaxHealth());
      mob.deathTime = 0;
      if (!tryPackIntoBell(level, mob)) {
         PersistentData.of(mob).putLong("JiangshiReviveDeadline", level.getGameTime() + 24000L);
         mob.setDeltaMovement(0.0, 0.0, 0.0);
      }
   }

   private static boolean tryPackIntoBell(ServerLevel level, Mob mob) {
      for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(mob) <= 4096.0)) {
         Inventory inventory = player.getInventory();

         for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof BronzeBellItem && !BronzeBellItem.hasMonster(stack)) {
               stack.set(ModDataComponents.BELL_CONTENT.get(), BronzeBellItem.pack(mob));
               inventory.setChanged();
               mob.discard();
               return true;
            }
         }
      }

      return false;
   }

   public static void tickReviveCountdown(LivingEntity living) {
      CompoundTag data = PersistentData.of(living);
      if (data.contains("JiangshiReviveDeadline")) {
         if (living.level() instanceof ServerLevel level) {
            if (level.getGameTime() >= data.getLongOr("JiangshiReviveDeadline", 0L)) {
               data.remove("JiangshiReviveDeadline");
               living.removeEffect(ModEffects.SUBDUED);
               WorkerData.setJob(living, WorkerJob.IDLE);
            }
         }
      }
   }
}
