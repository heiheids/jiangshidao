package com.heishan.jiangshidao.event;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.compat.PersistentData;
import com.heishan.jiangshidao.block.BronzeBellBlockEntity;
import net.minecraft.core.BlockPos;
import com.heishan.jiangshidao.registry.ModEffects;
import com.heishan.jiangshidao.worker.Talent;
import com.heishan.jiangshidao.worker.TalentSet;
import com.heishan.jiangshidao.worker.WorkerData;
import com.heishan.jiangshidao.worker.WorkerJob;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * SubduedEffectHandler —— 事件/任务处理。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class SubduedEffectHandler {
   private static final Map<LivingEntity, double[]> ANCHORS = Collections.synchronizedMap(new WeakHashMap<>());
   private static final int BURST_LENGTH = 40;
   private static final String FX_DONE = "JiangshiSubduedFxDone";
   private static final String FX_START = "JiangshiSubduedFxStart";

   private SubduedEffectHandler() {
   }

   public static void register() {
      ServerTickEvents.END_SERVER_TICK.register(SubduedEffectHandler::tickServer);
      ServerLivingEntityEvents.MOB_CONVERSION.register(SubduedEffectHandler::onConversion);
      // 降服单位是自家工人，不得伤害任何玩家。仅清 target 不足以拦住原版仇恨目标
      // 在同一刻重新锁定并发起的攻击，因此再加一道伤害拦截兜底。
      ServerLivingEntityEvents.ALLOW_DAMAGE.register(SubduedEffectHandler::allowDamage);
      // 绑定关系一直保持到怪物真正死亡；死亡时清掉铜铃上记录的绑定。
      ServerLivingEntityEvents.AFTER_DEATH.register(SubduedEffectHandler::onDeath);
   }

   private static void onDeath(LivingEntity entity, DamageSource source) {
      try {
         CompoundTag data = PersistentData.of(entity);
         if (!data.contains("JiangshiBellPos")) {
            return;
         }

         if (entity.level() instanceof ServerLevel level) {
            BlockPos bellPos = BlockPos.of(data.getLongOr("JiangshiBellPos", 0L));
            BronzeBellBlockEntity bell = BronzeBellBlockEntity.at(level, bellPos);
            if (bell != null && bell.isBound()) {
               bell.unbind();
            }
         }
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[铜铃] 死亡解绑失败，已跳过", t);
      }
   }

   private static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
      if (!(victim instanceof Player)) {
         return true;
      }

      if (source.getEntity() instanceof LivingEntity attacker && ModEffects.isSubdued(attacker)) {
         return false;
      }

      return true;
   }

   private static void tickServer(MinecraftServer server) {
      for (ServerLevel level : server.getAllLevels()) {
         try {
            tickLevel(level);
         } catch (Throwable t) {
            JiangshiDao.LOGGER.error("[降服] tick 异常，已跳过该维度", t);
         }
      }
   }

   private static void tickLevel(ServerLevel level) {
      for (Entity entity : level.getAllEntities()) {
         if (entity instanceof Mob mob) {
            try {
               tickMob(mob);
            } catch (Throwable t) {
               JiangshiDao.LOGGER.error("[降服] 单只生物处理失败，已跳过", t);
            }
         }
      }
   }

   private static void onConversion(Mob oldMob, Mob newMob, ConversionParams params) {
      if (oldMob != null && newMob != null && ModEffects.isSubdued(oldMob)) {
         try {
            copyWorkerState(oldMob, newMob);
            JiangshiDao.LOGGER.debug("[降服] 形态转换后状态已转移到新实体");
         } catch (Throwable t) {
            JiangshiDao.LOGGER.error("[降服] 形态转换补状态失败", t);
         }
      }
   }

   private static void copyWorkerState(LivingEntity from, LivingEntity to) {
      CompoundTag source = PersistentData.of(from);
      CompoundTag target = PersistentData.of(to);

      for (String key : source.keySet()) {
         Tag tag = source.get(key);
         if (tag != null) {
            target.put(key, tag.copy());
         }
      }

      to.addEffect(new MobEffectInstance(ModEffects.SUBDUED, -1, 0, false, true, true));
      PersistentData.flush(to);
      TalentEffectHandler.applyAttributeTalents(to);
   }

   private static void tickMob(Mob living) {
      if (!ModEffects.isSubdued(living) && WorkerData.ownerOf(living) != null && WorkerData.jobOf(living) != WorkerJob.IDLE) {
         living.addEffect(new MobEffectInstance(ModEffects.SUBDUED, -1, 0, false, true, true));
      }

      if (ModEffects.isSubdued(living)) {
         WorkerData.ensureName(living);
      }

      if (!ModEffects.isSubdued(living)) {
         ANCHORS.remove(living);
         CompoundTag data = PersistentData.of(living);
         data.remove("JiangshiSubduedFxDone");
         data.remove("JiangshiSubduedFxStart");
      } else {
         spawnSubduedParticles(living);
         TalentSet.ensureAssigned(living);
         TalentEffectHandler.tickReviveCountdown(living);

         // 被御尸符降服的单位已经是自家工人，不应再受"亡灵日光自燃"影响。
         // 降服后的僵尸/骷髅仍是原版亡灵类，天亮后会自行点燃并烧死，
         // 看起来就像"御尸符把僵尸烧了"。这里主动清火，等同永久遮阳。
         if (living.getRemainingFireTicks() > 0 || living.isOnFire()) {
            living.clearFire();
         }

         boolean working = WorkerData.jobOf(living) != WorkerJob.IDLE;

         // 工作状态下关闭生物自主 AI。
         // 原因：僵尸类自身带漫游（WaterAvoidingRandomStrollGoal）与跳跃冲量（tryHop），
         // 女鬼还带随机漂浮；这些每 tick 都在和工人的移动逻辑抢控制权，
         // 表现为"想靠近目标，却反复被拉向别处"。
         // 工人模式下移动与作业全部由 WorkerHandler 每 tick 驱动，不需要生物自主行为。
         living.setNoAi(working);
         if (!working) {
            living.getNavigation().stop();
         }

         // 降服单位只对敌对生物出手，不再把玩家当目标；同时清掉"被谁打了"的记仇，
         // 否则玩家不小心打到它，原版 HurtByTargetGoal 会立刻反打。
         if (living.getTarget() instanceof Player || living.getLastHurtByMob() instanceof Player) {
            living.setTarget(null);
            living.setLastHurtByMob(null);
         }
         if (!working) {
            if (!TalentSet.of(living).has(Talent.WAR)) {
               double[] anchor = ANCHORS.get(living);
               if (anchor == null) {
                  anchor = new double[]{living.getX(), living.getZ()};
                  ANCHORS.put(living, anchor);
               }

               // 只做"软约束"：离待命点超过 3 格才拉回，而不是每 tick 强制钉死。
               // 原先每 tick setPos 会把实体锁在原地，与寻路互相打架，
               // 表现成原地抖动、想走走不了。
               double dx = living.getX() - anchor[0];
               double dz = living.getZ() - anchor[1];
               if (dx * dx + dz * dz > 9.0) {
                  living.setPos(anchor[0], living.getY(), anchor[1]);
                  living.setDeltaMovement(0.0, living.getDeltaMovement().y, 0.0);
                  living.fallDistance = 0.0;
               }
            }
         }
      }
   }

   private static void spawnSubduedParticles(LivingEntity living) {
      if (living.level() instanceof ServerLevel server) {
         CompoundTag data = PersistentData.of(living);
         if (!data.getBooleanOr("JiangshiSubduedFxDone", false)) {
            long now = living.level().getGameTime();
            if (!data.contains("JiangshiSubduedFxStart")) {
               data.putLong("JiangshiSubduedFxStart", now);
            }

            long start = data.getLongOr("JiangshiSubduedFxStart", 0L);
            if (now - start > 40L) {
               data.putBoolean("JiangshiSubduedFxDone", true);
            } else {
               double spin = now * 0.35;
               double rise = 0.15 + now % 20L / 20.0 * 1.7;

               for (int i = 0; i < 3; i++) {
                  double angle = spin + i * (Math.PI * 2.0 / 3.0);
                  double offsetX = Math.cos(angle) * 0.75;
                  double offsetZ = Math.sin(angle) * 0.75;
                  server.sendParticles(
                     ParticleTypes.HAPPY_VILLAGER, living.getX() + offsetX, living.getY() + rise, living.getZ() + offsetZ, 1, 0.0, 0.0, 0.0, 0.0
                  );
               }
            }
         }
      }
   }
}
