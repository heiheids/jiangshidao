package com.heishan.jiangshidao.entity;

import com.heishan.jiangshidao.accessory.Accessories;
import com.heishan.jiangshidao.item.WoodSwordItem;
import com.heishan.jiangshidao.registry.ModEffects;
import com.heishan.jiangshidao.registry.ModEntities;
import com.heishan.jiangshidao.registry.ModItems;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

/**
 * FlyingSwordEntity —— 实体定义、刻逻辑与战斗行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class FlyingSwordEntity extends Projectile {
   private static final double MAX_DISTANCE = 50.0;
   private static final double RETURN_SPEED = 2.2;
   private static final double TRAIL_LENGTH = 2.0;
   private static final int TRAIL_PARTICLES = 6;
   private static final float DAMAGE_PERCENT = 0.45F;
   private FlyingSwordEntity.Phase phase = FlyingSwordEntity.Phase.OUT;
   private final Set<UUID> hitEntities = new HashSet<>();
   private Vec3 origin;
   private UUID ownerUuid;
   private WoodSwordItem.Variant variant = WoodSwordItem.Variant.PEACH;
   private int quiverSlot = -1;

   public FlyingSwordEntity(EntityType<? extends FlyingSwordEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
   }

   public int quiverSlot() {
      return this.quiverSlot;
   }

   public void setQuiverSlot(int slot) {
      this.quiverSlot = slot;
   }

   public FlyingSwordEntity(Level level, Player owner, ItemStack sword, WoodSwordItem.Variant variant) {
      super(ModEntities.FLYING_SWORD.get(), level);
      this.setOwner(owner);
      this.ownerUuid = owner.getUUID();
      this.origin = owner.position();
      this.variant = variant;
      this.noPhysics = true;
      this.setNoGravity(true);
   }

   public boolean isInvulnerable() {
      return true;
   }

   public boolean fireImmune() {
      return true;
   }

   public boolean isPeach() {
      return this.variant == WoodSwordItem.Variant.PEACH;
   }

   public WoodSwordItem.Variant swordVariant() {
      return this.variant;
   }

   public Item swordItem() {
      return switch (this.variant) {
         case PEACH -> (Item)ModItems.PEACH_WOOD_SWORD.get();
         case THUNDER -> (Item)ModItems.THUNDER_WOOD_SWORD.get();
         case TIAN_FA -> (Item)ModItems.TIAN_FA_LEI_JI_MU_SWORD.get();
      };
   }

   public void tick() {
      if (!this.level().isClientSide() && this.origin == null) {
         this.origin = this.position();
      }

      super.tick();
      if (this.level().isClientSide()) {
         this.spawnTrail();
      } else {
         if (this.phase == FlyingSwordEntity.Phase.OUT && this.origin != null && this.position().distanceTo(this.origin) >= 50.0) {
            this.startReturn();
         }

         if (this.phase == FlyingSwordEntity.Phase.RETURN) {
            this.updateReturn();
         } else {
            this.updateOutbound();
         }

         this.spawnTrail();
         if (this.tickCount > 600) {
            this.discard();
         }
      }
   }

   private void updateOutbound() {
      Vec3 from = this.position();
      Vec3 v = this.getDeltaMovement();
      Vec3 to = from.add(v);
      this.hitEntitiesAlong(from, to);
      BlockHitResult blockHit = this.level().clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, this));
      if (blockHit.getType() == Type.BLOCK) {
         this.setPos(blockHit.getLocation().x, blockHit.getLocation().y, blockHit.getLocation().z);
         this.startReturn();
      } else {
         this.setPos(to.x, to.y, to.z);
      }
   }

   private void hitEntitiesAlong(Vec3 from, Vec3 to) {
      AABB sweep = new AABB(from, to).inflate(0.8);
      List<Entity> candidates = this.level()
         .getEntities(this, sweep, e -> e != this && e.isAlive() && !this.hitEntities.contains(e.getUUID()) && e != this.getOwner());
      if (!candidates.isEmpty()) {
         int steps = Math.max(8, (int)Math.ceil(to.distanceTo(from) / 0.2));

         for (Entity candidate : candidates) {
            AABB box = candidate.getBoundingBox().inflate(0.3);

            for (int i = 0; i <= steps; i++) {
               double t = (double)i / steps;
               Vec3 p = from.add(to.subtract(from).scale(t));
               if (box.contains(p)) {
                  this.applyHit(candidate);
                  break;
               }
            }
         }
      }
   }

   private void applyHit(Entity target) {
      if (!this.hitEntities.contains(target.getUUID())) {
         LivingEntity owner = this.getOwner() instanceof LivingEntity le ? le : null;
         float dmg;
         if (target instanceof LivingEntity living) {
            dmg = living.getMaxHealth() * 0.45F;
         } else {
            dmg = 9.0F;
         }

         if (dmg < 0.5F) {
            dmg = 0.5F;
         }

         if (target instanceof LivingEntity livingTarget) {
            livingTarget.damageCooldownTime = 0;
         }

         boolean damaged = target.hurtOrSimulate(this.damageSources().thrown(this, owner), dmg);
         if (damaged) {
            this.hitEntities.add(target.getUUID());
            if (target.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD) && target instanceof LivingEntity living) {
               living.addEffect(new MobEffectInstance(ModEffects.SEALED, WoodSwordItem.sealTicksFor(this.variant), 0, false, true, true));
               if (this.level() instanceof ServerLevel server) {
                  server.sendParticles(
                     ParticleTypes.ENCHANTED_HIT, living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(), 10, 0.3, 0.3, 0.3, 0.1
                  );
               }
            }

            if (this.level() instanceof ServerLevel server) {
               server.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.5, target.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
            }
         }
      }
   }

   private void startReturn() {
      if (this.phase != FlyingSwordEntity.Phase.RETURN) {
         this.phase = FlyingSwordEntity.Phase.RETURN;
         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.6F, 1.4F);
      }
   }

   private void updateReturn() {
      LivingEntity owner = this.getOwner() instanceof LivingEntity le ? le : null;
      if (owner != null && owner.isAlive()) {
         Vec3 target = owner.getEyePosition().subtract(0.0, 0.3, 0.0);
         Vec3 toTarget = target.subtract(this.position());
         double dist = toTarget.length();
         if (dist < 1.5) {
            this.giveBackToOwner(owner);
         } else {
            Vec3 step = toTarget.normalize().scale(Math.min(2.2, dist));
            this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
            this.setDeltaMovement(step);
         }
      } else {
         this.discard();
      }
   }

   private void giveBackToOwner(LivingEntity owner) {
      if (owner instanceof Player player) {
         ItemStack sword = new ItemStack(this.swordItem());
         if (this.quiverSlot >= 0 && Accessories.returnToQuiver(player, this.quiverSlot, sword)) {
            this.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
            this.discard();
            return;
         }

         if (!player.getInventory().add(sword)) {
            net.minecraft.world.level.block.Block.popResource(player.level(), player.blockPosition(), sword);
         }

         this.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
      }

      this.discard();
   }

   private void spawnTrail() {
      Vec3 vel = this.getDeltaMovement();
      if (!(vel.lengthSqr() < 0.001)) {
         Vec3 back = vel.normalize().scale(-1.0);
         if (this.level() instanceof ServerLevel server) {
            for (int i = 0; i < 6; i++) {
               double t = i / 6.0;
               double d = t * 2.0;
               server.sendParticles(
                  ParticleTypes.WAX_ON, this.getX() + back.x * d, this.getY() + back.y * d, this.getZ() + back.z * d, 1, 0.02, 0.02, 0.02, 0.0
               );
            }
         }
      }
   }

   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide()) {
         this.applyHit(result.getEntity());
      }
   }

   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      if (!this.level().isClientSide()) {
         this.startReturn();
      }
   }

   protected void defineSynchedData(Builder builder) {
   }

   protected void readAdditionalSaveData(ValueInput tag) {
      super.readAdditionalSaveData(tag);
      this.phase = tag.getBooleanOr("Returning", false) ? FlyingSwordEntity.Phase.RETURN : FlyingSwordEntity.Phase.OUT;
      this.variant = WoodSwordItem.Variant.byId(tag.getStringOr("SwordVariant", ""));
      this.quiverSlot = tag.getIntOr("QuiverSlot", 0);
      this.origin = new Vec3(tag.getDoubleOr("OriginX", 0.0), tag.getDoubleOr("OriginY", 0.0), tag.getDoubleOr("OriginZ", 0.0));
      String uuidText = tag.getStringOr("OwnerUuid", "");
      if (!uuidText.isEmpty()) {
         try {
            this.ownerUuid = UUID.fromString(uuidText);
         } catch (IllegalArgumentException ignored) {
            this.ownerUuid = null;
         }
      }
   }

   protected void addAdditionalSaveData(ValueOutput tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("Returning", this.phase == FlyingSwordEntity.Phase.RETURN);
      tag.putString("SwordVariant", this.variant.id());
      tag.putInt("QuiverSlot", this.quiverSlot);
      if (this.origin != null) {
         tag.putDouble("OriginX", this.origin.x);
         tag.putDouble("OriginY", this.origin.y);
         tag.putDouble("OriginZ", this.origin.z);
      }

      if (this.ownerUuid != null) {
         tag.putString("OwnerUuid", this.ownerUuid.toString());
      }
   }

   private enum Phase {
      OUT,
      RETURN;
   }
}
