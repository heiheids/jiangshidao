package com.heishan.jiangshidao.entity;

import com.heishan.jiangshidao.registry.ModEffects;
import com.heishan.jiangshidao.registry.ModEntities;
import com.heishan.jiangshidao.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * SealingTalismanEntity —— 实体定义、刻逻辑与战斗行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class SealingTalismanEntity extends ThrowableItemProjectile {
   private static final double MAX_DISTANCE = 100.0;
   private static final int SEAL_DURATION_TICKS = 400;
   private Vec3 origin;

   public SealingTalismanEntity(EntityType<? extends SealingTalismanEntity> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   public SealingTalismanEntity(Level level, LivingEntity shooter) {
      super(ModEntities.SEALING_TALISMAN.get(), shooter, level, new ItemStack((ItemLike)ModItems.SEALING_TALISMAN.get()));
      this.setNoGravity(true);
      this.origin = shooter.position();
   }

   public SealingTalismanEntity(Level level, double x, double y, double z) {
      super(ModEntities.SEALING_TALISMAN.get(), x, y, z, level, new ItemStack((ItemLike)ModItems.SEALING_TALISMAN.get()));
      this.setNoGravity(true);
      this.origin = new Vec3(x, y, z);
   }

   protected Item getDefaultItem() {
      return ModItems.SEALING_TALISMAN.get();
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         if (this.origin == null) {
            this.origin = this.position();
         }

         if (this.position().distanceTo(this.origin) > 100.0) {
            this.discardWithParticles();
            return;
         }

         if (this.level() instanceof ServerLevel server && this.tickCount % 2 == 0) {
            server.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY(), this.getZ(), 2, 0.1, 0.1, 0.1, 0.02);
         }
      }
   }

   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide()) {
         if (result.getEntity() instanceof LivingEntity living) {
            if (!isUndeadEntity(living)) {
               this.discardWithParticles();
            } else {
               living.addEffect(new MobEffectInstance(ModEffects.SEALED, 400, 0, false, true, true));
               if (living instanceof Mob mob) {
                  mob.getNavigation().stop();
                  mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
               }

               if (this.level() instanceof ServerLevel server) {
                  server.sendParticles(ParticleTypes.CRIT, living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(), 12, 0.3, 0.3, 0.3, 0.1);
               }

               this.discardWithParticles();
            }
         } else {
            this.discardWithParticles();
         }
      }
   }

   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      this.discardWithParticles();
   }

   private void discardWithParticles() {
      if (this.level() instanceof ServerLevel server) {
         server.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 6, 0.15, 0.15, 0.15, 0.01);
      }

      this.discard();
   }

   public static boolean isUndeadEntity(Entity entity) {
      return entity.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD);
   }

   public static boolean isEnemy(Entity entity) {
      return entity instanceof Enemy;
   }
}
