package com.heishan.jiangshidao.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * JiangshiEntity —— 实体定义、刻逻辑与战斗行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class JiangshiEntity extends Zombie {
   public static final float JUMP_HEIGHT_BLOCKS = 2.0F;
   private static final double JUMP_STRENGTH_FOR_2_BLOCKS = 0.5449;
   private static final int JUMP_INTERVAL = 12;
   private final JiangshiType jiangshiType;
   private int jumpCooldown;

   public JiangshiEntity(EntityType<? extends JiangshiEntity> entityType, Level level, JiangshiType type) {
      super(entityType, level);
      this.jiangshiType = type;
   }

   public JiangshiType jiangshiType() {
      return this.jiangshiType;
   }

   public static Builder createAttributesInternal(JiangshiType type) {
      return Zombie.createAttributes()
         .add(Attributes.MAX_HEALTH, type.maxHealth())
         .add(Attributes.ATTACK_DAMAGE, type.attackDamage())
         .add(Attributes.MOVEMENT_SPEED, type.movementSpeed())
         .add(Attributes.JUMP_STRENGTH, 0.5449);
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         if (this.jumpCooldown > 0) {
            this.jumpCooldown--;
         }

         this.tryHop();
      }
   }

   private void tryHop() {
      if (this.jumpCooldown <= 0 && this.onGround()) {
         boolean pathing = !this.getNavigation().isDone();
         boolean chasing = this.getTarget() != null && this.getTarget().isAlive();
         if (pathing || chasing) {
            this.jumpFromGround();
            this.applyHopImpulse();
            this.jumpCooldown = 12;
         }
      }
   }

   private void applyHopImpulse() {
      float yaw = this.getYRot() * (float) (Math.PI / 180.0);
      double dx = -Mth.sin(yaw);
      double dz = Mth.cos(yaw);
      double power = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.85;
      MobEffectInstance slow = this.getEffect(MobEffects.SLOWNESS);
      if (slow != null) {
         double factor = Math.max(0.0, 1.0 - 0.15 * (slow.getAmplifier() + 1));
         power *= factor;
      }

      Vec3 v = this.getDeltaMovement();
      this.setDeltaMovement(v.x + dx * power, v.y, v.z + dz * power);
   }

   public void travel(Vec3 movementInput) {
      if (!this.isInWater() && !this.isInLava() && this.onGround()) {
         super.travel(Vec3.ZERO);
      } else {
         super.travel(movementInput);
      }
   }

   public boolean doHurtTarget(ServerLevel level, Entity target) {
      boolean hit = super.doHurtTarget(level, target);
      if (hit && target instanceof LivingEntity living && !this.level().isClientSide()) {
         living.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1), this);
      }

      return hit;
   }

   protected boolean isSunSensitive() {
      return !this.jiangshiType.sunImmune();
   }

   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      boolean damaged = super.hurtServer(level, source, amount);
      if (damaged && !this.level().isClientSide()) {
         this.spreadAnger(source.getEntity());
      }

      return damaged;
   }

   private void spreadAnger(Entity attacker) {
      if (attacker instanceof LivingEntity livingAttacker) {
         if (!(livingAttacker instanceof JiangshiEntity)) {
            double radius = 16.0;

            for (JiangshiEntity other : this.level().getEntitiesOfClass(JiangshiEntity.class, this.getBoundingBox().inflate(radius, 8.0, radius))) {
               if (other != this && !other.isAlliedTo(livingAttacker) && other.getTarget() == null) {
                  other.setTarget(livingAttacker);
                  other.setLastHurtByMob(livingAttacker);
               }
            }
         }
      }
   }

   public boolean canBreakDoors() {
      return false;
   }

   protected boolean isAlwaysExperienceDropper() {
      return true;
   }

   public boolean isBaby() {
      return false;
   }

   public boolean canPickUpLoot() {
      return false;
   }

   public boolean removeWhenFarAway(double distance) {
      return true;
   }

   protected boolean convertsInWater() {
      return false;
   }
}
