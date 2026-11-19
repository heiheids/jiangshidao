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
 * 鬼女移动控制：贴地/飞行切换与减速处理
 * <p>自 {@link GhostWomanEntity} 原样抽出。
 */
final class GhostMoveControl extends MoveControl {
      private final GhostWomanEntity ghost;

      GhostMoveControl(GhostWomanEntity ghost) {
         super(ghost);
         this.ghost = ghost;
      }

      boolean isFlying() {
         return this.operation == Operation.MOVE_TO;
      }

      public void tick() {
         if (this.operation == Operation.MOVE_TO) {
            Vec3 delta = new Vec3(this.wantedX - this.ghost.getX(), this.wantedY - this.ghost.getY(), this.wantedZ - this.ghost.getZ());
            double dist = delta.length();
            if (dist < this.ghost.getBoundingBox().getSize()) {
               this.operation = Operation.WAIT;
               this.ghost.setDeltaMovement(this.ghost.getDeltaMovement().scale(0.5));
            } else {
               double slowFactor = 1.0;
               MobEffectInstance slow = this.ghost.getEffect(MobEffects.SLOWNESS);
               if (slow != null) {
                  slowFactor = Math.max(0.0, 1.0 - 0.15 * (slow.getAmplifier() + 1));
               }

               double powerFactor = this.ghost.variant().powerMultiplier();
               this.ghost.setDeltaMovement(this.ghost.getDeltaMovement().add(delta.scale(this.speedModifier * 0.05 * slowFactor * powerFactor / dist)));
               LivingEntity target = this.ghost.getTarget();
               if (target != null) {
                  double dx = target.getX() - this.ghost.getX();
                  double dz = target.getZ() - this.ghost.getZ();
                  this.ghost.setYRot(-((float)Mth.atan2(dx, dz)) * (180.0F / (float)Math.PI));
               } else {
                  Vec3 v = this.ghost.getDeltaMovement();
                  this.ghost.setYRot(-((float)Mth.atan2(v.x, v.z)) * (180.0F / (float)Math.PI));
               }

               this.ghost.yBodyRot = this.ghost.getYRot();
            }
         }
      }
   }
