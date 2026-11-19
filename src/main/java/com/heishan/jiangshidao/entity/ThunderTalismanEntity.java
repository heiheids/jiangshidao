package com.heishan.jiangshidao.entity;

import com.heishan.jiangshidao.registry.ModEntities;
import com.heishan.jiangshidao.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * ThunderTalismanEntity —— 实体定义、刻逻辑与战斗行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class ThunderTalismanEntity extends ThrowableItemProjectile {
   private static final double RADIUS = 3.0;
   private static final double MAX_DISTANCE = 100.0;
   public static final float DAMAGE_PERCENT = 0.2F;
   public static final float UNDEAD_BONUS = 0.3F;
   private Vec3 origin;
   private boolean detonated;

   public ThunderTalismanEntity(EntityType<? extends ThunderTalismanEntity> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   public ThunderTalismanEntity(Level level, LivingEntity shooter) {
      super(ModEntities.THUNDER_TALISMAN.get(), shooter, level, new ItemStack((ItemLike)ModItems.THUNDER_TALISMAN.get()));
      this.setNoGravity(true);
      this.origin = shooter.position();
   }

   public ThunderTalismanEntity(Level level, double x, double y, double z) {
      super(ModEntities.THUNDER_TALISMAN.get(), x, y, z, level, new ItemStack((ItemLike)ModItems.THUNDER_TALISMAN.get()));
      this.setNoGravity(true);
      this.origin = new Vec3(x, y, z);
   }

   protected Item getDefaultItem() {
      return ModItems.THUNDER_TALISMAN.get();
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         if (this.origin == null) {
            this.origin = this.position();
         }

         if (this.position().distanceTo(this.origin) > 100.0) {
            this.detonate(this.position());
         } else {
            if (this.level() instanceof ServerLevel server && this.tickCount % 2 == 0) {
               server.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 2, 0.08, 0.08, 0.08, 0.02);
            }
         }
      }
   }

   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide()) {
         this.detonate(result.getEntity().position());
      }
   }

   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      if (!this.level().isClientSide()) {
         this.detonate(result.getLocation());
      }
   }

   private void detonate(Vec3 center) {
      if (!this.detonated) {
         this.detonated = true;
         if (this.level() instanceof ServerLevel server) {
            strike(server, center, this.getOwner());
            this.discard();
         } else {
            this.discard();
         }
      }
   }

   public static void strike(ServerLevel server, Vec3 center, Entity owner) {
      LightningBolt bolt = new LightningBolt(EntityTypes.LIGHTNING_BOLT, server);
      bolt.setVisualOnly(true);
      bolt.setPosRaw(center.x, center.y, center.z);
      server.addFreshEntity(bolt);
      AABB area = new AABB(center.x - 3.0, center.y - 3.0, center.z - 3.0, center.x + 3.0, center.y + 3.0, center.z + 3.0);

      for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && e != owner)) {
         if (!(living.position().distanceTo(center) > 3.0)) {
            float damage = living.getMaxHealth() * 0.2F;
            if (isUndead(living)) {
               damage *= 1.3F;
            }

            if (damage < 0.5F) {
               damage = 0.5F;
            }

            living.damageCooldownTime = 0;
            living.hurt(server.damageSources().lightningBolt(), damage);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(), 14, 0.3, 0.4, 0.3, 0.1);
         }
      }

      spawnSparkParticles(server, center);
      server.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.2F, 1.0F);
      server.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 1.0F, 1.2F);
   }

   private static void spawnSparkParticles(ServerLevel server, Vec3 center) {
      int SPARK_COUNT = 60;

      for (int i = 0; i < 60; i++) {
         double phi = Math.acos(1.0 - 2.0 * ((i + 0.5) / 60.0));
         double theta = Math.PI * (1.0 + Math.sqrt(5.0)) * i;
         double r = 3.0 * (0.2 + 0.8 * (i * 41 % 100 / 100.0));
         double dx = Math.sin(phi) * Math.cos(theta) * r;
         double dy = Math.cos(phi) * r * 0.45;
         double dz = Math.sin(phi) * Math.sin(theta) * r;
         server.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x + dx, center.y + 0.4 + dy, center.z + dz, 1, 0.02, 0.02, 0.02, 0.05);
      }
   }

   private static boolean isUndead(LivingEntity entity) {
      return entity.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD);
   }
}
