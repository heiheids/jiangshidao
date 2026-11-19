package com.heishan.jiangshidao.entity;

import com.heishan.jiangshidao.item.SubjugationTalismanItem;
import com.heishan.jiangshidao.registry.ModEffects;
import com.heishan.jiangshidao.registry.ModEntities;
import com.heishan.jiangshidao.registry.ModItems;
import com.heishan.jiangshidao.worker.WorkerData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * SubjugationTalismanEntity —— 实体定义、刻逻辑与战斗行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class SubjugationTalismanEntity extends ThrowableItemProjectile {
   private static final double MAX_DISTANCE = 100.0;
   private Vec3 origin;

   public SubjugationTalismanEntity(EntityType<? extends SubjugationTalismanEntity> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   public SubjugationTalismanEntity(Level level, LivingEntity shooter) {
      super(ModEntities.SUBJUGATION_TALISMAN.get(), shooter, level, new ItemStack((ItemLike)ModItems.SUBJUGATION_TALISMAN.get()));
      this.setNoGravity(true);
      this.origin = shooter.position();
   }

   public SubjugationTalismanEntity(Level level, double x, double y, double z) {
      super(ModEntities.SUBJUGATION_TALISMAN.get(), x, y, z, level, new ItemStack((ItemLike)ModItems.SUBJUGATION_TALISMAN.get()));
      this.setNoGravity(true);
      this.origin = new Vec3(x, y, z);
   }

   protected Item getDefaultItem() {
      return ModItems.SUBJUGATION_TALISMAN.get();
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         if (this.origin == null) {
            this.origin = this.position();
         }

         if (this.position().distanceTo(this.origin) > 100.0) {
            this.discardWithParticles();
         } else {
            if (this.level() instanceof ServerLevel server && this.tickCount % 2 == 0) {
               server.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY(), this.getZ(), 2, 0.1, 0.1, 0.1, 0.02);
            }
         }
      }
   }

   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide()) {
         if (result.getEntity() instanceof LivingEntity living && SubjugationTalismanItem.isSubduable(living)) {
            float chance = SubjugationTalismanItem.chanceFor(living);
            boolean success = this.random.nextFloat() < chance;
            if (success) {
               living.addEffect(new MobEffectInstance(ModEffects.SUBDUED, -1, 0, false, true, true));
               if (this.getOwner() instanceof Player thrower) {
                  WorkerData.setOwner(living, thrower.getUUID());
               }

               if (living instanceof Mob mob) {
                  mob.getNavigation().stop();
                  mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
               }
            }

            if (this.level() instanceof ServerLevel server) {
               server.sendParticles(
                  success ? ParticleTypes.CRIT : ParticleTypes.SMOKE,
                  living.getX(),
                  living.getY() + living.getBbHeight() * 0.5,
                  living.getZ(),
                  12,
                  0.3,
                  0.3,
                  0.3,
                  0.1
               );
               server.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 0.8F, success ? 1.4F : 0.7F);
            }

            this.discardWithParticles();
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
}
