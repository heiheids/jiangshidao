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
 * 鬼女随机游走目标
 * <p>自 {@link GhostWomanEntity} 原样抽出。
 */
final class GhostRandomMoveGoal extends Goal {
      private final GhostWomanEntity ghost;

      GhostRandomMoveGoal(GhostWomanEntity ghost) {
         this.ghost = ghost;
         this.setFlags(EnumSet.of(Flag.MOVE));
      }

      public boolean canUse() {
         if (this.ghost.isInSunlight()) {
            return false;
         } else if (((GhostMoveControl)this.ghost.getMoveControl()).isFlying()) {
            return false;
         } else {
            return this.ghost.getTarget() != null ? false : this.ghost.getRandom().nextInt(reducedTickDelay(5)) == 0;
         }
      }

      public boolean canContinueToUse() {
         return false;
      }

      public void start() {
         int groundY = this.ghost.level().getHeight(Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(this.ghost.getX()), Mth.floor(this.ghost.getZ()));
         double ox = (this.ghost.getRandom().nextDouble() - 0.5) * 16.0;
         double oz = (this.ghost.getRandom().nextDouble() - 0.5) * 16.0;
         double oy = 1.2 + this.ghost.getRandom().nextDouble() * 2.8;
         this.ghost.getMoveControl().setWantedPosition(this.ghost.getX() + ox, groundY + oy, this.ghost.getZ() + oz, 1.0);
      }
   }
