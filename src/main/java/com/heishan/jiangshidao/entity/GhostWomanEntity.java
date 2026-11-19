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
 * GhostWomanEntity —— 实体定义、刻逻辑与战斗行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class GhostWomanEntity extends Zombie {
   public static final double MAX_FLY_HEIGHT = 4.0;
   private static final double MIN_FLY_HEIGHT = 1.2;
   private static final double ATTACK_RANGE_SQR = 4.84;
   private int attackCooldown;
   private final GhostWomanVariant variant;
   private static final double BASE_ATTACK_DAMAGE = 3.0;
   private static final double BASE_MOVEMENT_SPEED = 0.3;
   private static final double BASE_FLYING_SPEED = 0.3;
   private static final boolean DEBUG_LOG = true;
   private static final double REVEAL_RADIUS = 6.0;

   public GhostWomanEntity(EntityType<? extends GhostWomanEntity> entityType, Level level, GhostWomanVariant variant) {
      super(entityType, level);
      this.variant = variant;
      // 恢复女鬼自带的飞行移动控制 GhostMoveControl：
      // 它本身就是三维飞行实现（朝向目标直接位移，含上下），
      // 而且 GhostRandomMoveGoal 等处会把它强转为 GhostMoveControl，
      // 换成 FlyingMoveControl 会触发 ClassCastException 导致服务器崩溃。
      this.moveControl = new GhostMoveControl(this);
      this.xpReward = 5;
   }

   public GhostWomanVariant variant() {
      return this.variant;
   }

   protected PathNavigation createNavigation(Level level) {
      // 保持地面寻路：这是父类 Zombie 的硬性前提（见上方构造注释）。
      // 飞行能力由 FlyingMoveControl 提供，不影响上下移动。
      return new GroundPathNavigation(this, level);
   }

   public static Builder createAttributes() {
      return createAttributes(GhostWomanVariant.WHITE);
   }

   public static Builder createAttributes(GhostWomanVariant variant) {
      double m = variant.powerMultiplier();
      return Zombie.createAttributes()
         .add(Attributes.MAX_HEALTH, 20.0)
         .add(Attributes.ATTACK_DAMAGE, 3.0 * m)
         .add(Attributes.MOVEMENT_SPEED, 0.3 * m)
         .add(Attributes.JUMP_STRENGTH, 0.0)
         .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.0)
         .add(Attributes.FLYING_SPEED, 0.3 * m);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(4, new GhostChargeGoal(this));
      this.goalSelector.addGoal(8, new GhostRandomMoveGoal(this));
      this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]).setAlertOthers(new Class[]{GhostWomanEntity.class}));
      this.targetSelector.addGoal(2, new GhostTargetPlayerGoal(this));
   }

   protected void addBehaviourGoals() {
   }

   public void tick() {
      super.tick();
      if (this.attackCooldown > 0) {
         this.attackCooldown--;
      }

      if (!this.level().isClientSide()) {
         this.clampFlightHeight();
         this.updateVisibility();
         this.acquireTargetIfNeeded();
         if (this.tickCount % 40 == 0) {
            LivingEntity t = this.getTarget();
            Player nearest = this.level().getNearestPlayer(this, 40.0);
            double nearestDist = nearest == null ? -1.0 : Math.sqrt(this.distanceToSqr(nearest));
            boolean flying = ((GhostMoveControl)this.getMoveControl()).isFlying();
            JiangshiDao.LOGGER
               .info(
                  "[GhostWoman] pos=({}, {}, {}) target={} nearestPlayerDist={} flying={} navDone={}",
                  new Object[]{
                     String.format("%.1f", this.getX()),
                     String.format("%.1f", this.getY()),
                     String.format("%.1f", this.getZ()),
                     t == null ? "null" : t.getName().getString(),
                     nearestDist < 0.0 ? "none" : String.format("%.1f", nearestDist),
                     flying,
                     this.getNavigation().isDone()
                  }
               );
         }

         LivingEntity target = this.getTarget();
         if (target != null && target.isAlive() && this.distanceToSqr(target) <= 4.84 && this.attackCooldown <= 0) {
            this.doHurtTarget((ServerLevel)this.level(), target);
            this.attackCooldown = 20;
         }
      }
   }

   private void acquireTargetIfNeeded() {
   }

   private void clampFlightHeight() {
      boolean sealed = this.hasEffect(ModEffects.SEALED);
      int footX = Mth.floor(this.getX());
      int footZ = Mth.floor(this.getZ());
      int groundY = this.level().getHeight(Types.MOTION_BLOCKING_NO_LEAVES, footX, footZ);
      double maxY = groundY + 4.0;
      double minY = groundY + 1.2;
      Vec3 v = this.getDeltaMovement();
      if (this.getY() > maxY) {
         double vy = Math.min(v.y, -0.08);
         this.setDeltaMovement(v.x, vy, v.z);
         this.setPos(this.getX(), Math.min(this.getY(), maxY + 0.1), this.getZ());
      } else if (this.getY() < minY) {
         if (sealed) {
            if (v.y > 0.0) {
               this.setDeltaMovement(v.x, 0.0, v.z);
            }
         } else {
            this.setDeltaMovement(v.x, Math.max(v.y, 0.08), v.z);
         }
      }

      this.fallDistance = 0.0;
   }

   public boolean doHurtTarget(ServerLevel level, Entity target) {
      boolean hit = super.doHurtTarget(level, target);
      if (hit && target instanceof LivingEntity living && !this.level().isClientSide()) {
         living.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0), this);
         living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1), this);
      }

      return hit;
   }

   protected boolean isSunSensitive() {
      return false;
   }

   public boolean isInSunlight() {
      long timeOfDay = this.level().getOverworldClockTime() % 24000L;
      if (timeOfDay >= 12000L) {
         return false;
      } else {
         return this.level().isRainingAt(this.blockPosition()) ? false : this.level().canSeeSky(this.blockPosition().above());
      }
   }

   private boolean hasPlayerWithinRevealRadius() {
      AABB area = this.getBoundingBox().inflate(6.0);

      for (Player p : this.level().getEntitiesOfClass(Player.class, area)) {
         if (p.isAlive() && !p.isSpectator() && p.distanceToSqr(this) <= 36.0) {
            return true;
         }
      }

      return false;
   }

   public boolean shouldBeVisible() {
      return ModEffects.isSubdued(this) ? true : this.hasPlayerWithinRevealRadius();
   }

   private void updateVisibility() {
      boolean visible = this.shouldBeVisible();
      if (this.isInvisible() == visible) {
         this.setInvisible(!visible);
      }

      if (this.isInSunlight() && this.getTarget() != null) {
         this.setTarget(null);
      }
   }

   public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
      return false;
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

   protected boolean convertsInWater() {
      return false;
   }

   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      boolean damaged = super.hurtServer(level, source, amount);
      if (damaged && !this.level().isClientSide() && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof GhostWomanEntity)) {
         for (GhostWomanEntity other : this.level().getEntitiesOfClass(GhostWomanEntity.class, this.getBoundingBox().inflate(12.0, 8.0, 12.0))) {
            if (other != this && other.getTarget() == null) {
               other.setTarget(attacker);
               other.setLastHurtByMob(attacker);
            }
         }
      }

      return damaged;
   }

   public static double maxFlyHeight() {
      return 4.0;
   }

   public static GhostWomanGoalStats stats() {
      return new GhostWomanGoalStats();
   }





}