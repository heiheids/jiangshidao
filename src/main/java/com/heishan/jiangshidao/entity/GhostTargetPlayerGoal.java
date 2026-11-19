package com.heishan.jiangshidao.entity;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.registry.ModEffects;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 鬼女索敌目标：按可见性与半径锁定玩家
 * <p>自 {@link GhostWomanEntity} 原样抽出。
 */
final class GhostTargetPlayerGoal extends TargetGoal {
      private final GhostWomanEntity ghost;
      private static final double DETECT_RADIUS = 6.0;

      GhostTargetPlayerGoal(GhostWomanEntity ghost) {
         super(ghost, false);
         this.ghost = ghost;
         this.setFlags(EnumSet.of(Flag.TARGET));
      }

      private double radius() {
         return this.ghost.getAttributeValue(Attributes.FOLLOW_RANGE);
      }

      public boolean canUse() {
         GhostWomanGoalStats.canUseCalls++;
         // 被御尸符降服的鬼女是自家工人，不能在索敌阶段就锁定玩家；
         // 否则目标每 tick 被重新抢回，仅靠事后 setTarget(null) 拦不住。
         if (ModEffects.isSubdued(this.ghost)) {
            return false;
         }

         if (this.ghost.isInSunlight()) {
            return false;
         }

         Player best = this.findNearestPlayer();
         if (best == null) {
            return false;
         }

         GhostWomanGoalStats.foundPlayers++;
         this.ghost.setTarget(best);
         if (this.ghost.getTarget() == best) {
            GhostWomanGoalStats.setTargetCalls++;
         }

         return true;
      }

      public boolean canContinueToUse() {
         LivingEntity t = this.ghost.getTarget();
         double r = this.radius();
         return t != null && t.isAlive() && this.ghost.distanceToSqr(t) <= r * r;
      }

      private Player findNearestPlayer() {
         double r = 6.0;
         AABB area = this.ghost.getBoundingBox().inflate(r);
         List<Player> players = this.ghost.level().getEntitiesOfClass(Player.class, area);
         Player best = null;
         double bestDist = Double.MAX_VALUE;

         for (Player p : players) {
            if (p.isAlive() && !p.isSpectator() && !p.isCreative() && !p.isInvulnerable()) {
               double d = this.ghost.distanceToSqr(p);
               if (d < bestDist && d <= r * r) {
                  bestDist = d;
                  best = p;
               }
            }
         }

         return best;
      }
   }
