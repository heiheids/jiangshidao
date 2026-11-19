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
 * 鬼女冲锋目标：接近玩家并攻击
 * <p>自 {@link GhostWomanEntity} 原样抽出。
 */
final class GhostChargeGoal extends Goal {
      private final GhostWomanEntity ghost;

      GhostChargeGoal(GhostWomanEntity ghost) {
         this.ghost = ghost;
         this.setFlags(EnumSet.of(Flag.MOVE));
      }

      public boolean canUse() {
         if (this.ghost.isInSunlight()) {
            return false;
         }

         LivingEntity target = this.ghost.getTarget();
         return target != null && target.isAlive() ? this.ghost.distanceToSqr(target) > 4.84 : false;
      }

      public boolean canContinueToUse() {
         LivingEntity target = this.ghost.getTarget();
         return target != null && target.isAlive() && this.ghost.distanceToSqr(target) > 4.84;
      }

      public void start() {
         this.updateWanted();
      }

      public void tick() {
         this.updateWanted();
      }

      private void updateWanted() {
         LivingEntity target = this.ghost.getTarget();
         if (target != null) {
            double eyeY = target.getEyeY();
            int groundY = this.ghost.level().getHeight(Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(this.ghost.getX()), Mth.floor(this.ghost.getZ()));
            double cappedY = Math.min(eyeY, groundY + 4.0);
            this.ghost.getMoveControl().setWantedPosition(target.getX(), cappedY, target.getZ(), 1.0);
         }
      }

      public void stop() {
         this.ghost.setDeltaMovement(this.ghost.getDeltaMovement().scale(0.8));
      }

      public boolean requiresUpdateEveryTick() {
         return true;
      }
   }
