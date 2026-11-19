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
 * BurningTalismanEntity —— 实体定义、刻逻辑与战斗行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class BurningTalismanEntity extends ThrowableItemProjectile {
   private static final double RADIUS = 3.0;
   private static final double MAX_DISTANCE = 100.0;
   public static final float LAVA_DAMAGE = 4.0F;
   public static final int LAVA_FIRE_TICKS = 300;
   public static final float UNDEAD_BONUS = 0.3F;
   private Vec3 origin;
   private boolean detonated;

   public BurningTalismanEntity(EntityType<? extends BurningTalismanEntity> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   public BurningTalismanEntity(Level level, LivingEntity shooter) {
      super(ModEntities.BURNING_TALISMAN.get(), shooter, level, new ItemStack((ItemLike)ModItems.BURNING_TALISMAN.get()));
      this.setNoGravity(true);
      this.origin = shooter.position();
   }

   public BurningTalismanEntity(Level level, double x, double y, double z) {
      super(ModEntities.BURNING_TALISMAN.get(), x, y, z, level, new ItemStack((ItemLike)ModItems.BURNING_TALISMAN.get()));
      this.setNoGravity(true);
      this.origin = new Vec3(x, y, z);
   }

   protected Item getDefaultItem() {
      return ModItems.BURNING_TALISMAN.get();
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
               server.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 2, 0.08, 0.08, 0.08, 0.01);
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
            igniteArea(server, center, this.getOwner());
            this.discard();
         } else {
            this.discard();
         }
      }
   }

   public static void igniteArea(ServerLevel server, Vec3 center, Entity owner) {
      AABB area = new AABB(center.x - 3.0, center.y - 3.0, center.z - 3.0, center.x + 3.0, center.y + 3.0, center.z + 3.0);

      for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && e != owner)) {
         if (!(living.position().distanceTo(center) > 3.0) && !living.fireImmune()) {
            living.igniteForTicks(300);
            float damage = isUndead(living) ? 5.2F : 4.0F;
            living.hurt(server.damageSources().lava(), damage);
         }
      }

      spawnFireParticles(server, center);
      server.playSound(null, center.x, center.y, center.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
      server.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_BURN, SoundSource.PLAYERS, 0.8F, 1.2F);
   }

   private static void spawnFireParticles(ServerLevel server, Vec3 center) {
      int FLAME_COUNT = 70;
      int LAVA_COUNT = 12;
      int SMOKE_COUNT = 20;

      for (int i = 0; i < 70; i++) {
         double phi = Math.acos(1.0 - 2.0 * ((i + 0.5) / 70.0));
         double theta = Math.PI * (1.0 + Math.sqrt(5.0)) * i;
         double r = 3.0 * (0.35 + 0.65 * (i * 37 % 100 / 100.0));
         double dx = Math.sin(phi) * Math.cos(theta) * r;
         double dy = Math.cos(phi) * r * 0.6;
         double dz = Math.sin(phi) * Math.sin(theta) * r;
         server.sendParticles(ParticleTypes.FLAME, center.x + dx, center.y + 0.3 + dy, center.z + dz, 1, 0.02, 0.02, 0.02, 0.01);
      }

      for (int i = 0; i < 12; i++) {
         double angle = i / 12.0 * Math.PI * 2.0;
         double r = 2.4000000000000004;
         server.sendParticles(ParticleTypes.LAVA, center.x + Math.cos(angle) * r, center.y + 0.4, center.z + Math.sin(angle) * r, 1, 0.0, 0.0, 0.0, 0.0);
      }

      for (int i = 0; i < 20; i++) {
         double angle = i / 20.0 * Math.PI * 2.0;
         double r = 3.0 * (0.3 + 0.7 * (i * 53 % 100 / 100.0));
         server.sendParticles(
            ParticleTypes.LARGE_SMOKE, center.x + Math.cos(angle) * r, center.y + 0.6, center.z + Math.sin(angle) * r, 1, 0.05, 0.05, 0.05, 0.01
         );
      }
   }

   private static boolean isUndead(LivingEntity entity) {
      return entity.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD);
   }
}
