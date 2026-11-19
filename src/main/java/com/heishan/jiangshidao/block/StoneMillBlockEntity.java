package com.heishan.jiangshidao.block;

import com.heishan.jiangshidao.registry.ModBlockEntities;
import com.heishan.jiangshidao.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * StoneMillBlockEntity —— 方块实体，负责该方块的状态与刻逻辑。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class StoneMillBlockEntity extends BlockEntity {
   public static final int MAX_STORED = 64;
   private static final float SPIN_SPEED = 24.0F;
   private static final int HOLD_FRESH_TICKS = 8;
   private ItemStack stored = ItemStack.EMPTY;
   private float angle;
   private float progress;
   private int ticksSinceHold = Integer.MAX_VALUE;
   private float lastSyncedAngle;
   private boolean wasSpinning;

   public StoneMillBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.STONE_MILL.get(), pos, state);
   }

   public boolean tryInsert(ItemStack held) {
      if (!isValidInput(held)) {
         return false;
      }

      if (this.stored.isEmpty()) {
         this.stored = new ItemStack(held.getItem(), 1);
         this.markUpdated();
         return true;
      }

      if (this.stored.is(held.getItem())) {
         if (this.stored.getCount() >= 64) {
            return false;
         }

         this.stored.grow(1);
         this.markUpdated();
         return true;
      } else {
         return false;
      }
   }

   public static boolean isValidInput(ItemStack stack) {
      return !stack.isEmpty() && (stack.is(Items.SULFUR) || stack.is(Items.CINNABAR));
   }

   public ItemStack stored() {
      return this.stored;
   }

   public int storedCount() {
      return this.stored.getCount();
   }

   public String statusText() {
      if (this.stored.isEmpty()) {
         return "§7空";
      }

      String name = this.stored.getHoverName().getString();
      return "§e" + name + " §f" + this.stored.getCount() + "/64";
   }

   public static StoneMillBlockEntity at(Level level, BlockPos pos) {
      if (level == null) {
         return null;
      } else {
         return level.getBlockEntity(pos) instanceof StoneMillBlockEntity mill ? mill : null;
      }
   }

   public void onHoldPulse() {
      this.ticksSinceHold = 0;
      if (this.level != null && !this.level.isClientSide()) {
         this.markUpdated();
      }
   }

   public void tick() {
      if (this.ticksSinceHold < Integer.MAX_VALUE) {
         this.ticksSinceHold++;
      }

      boolean holding = this.ticksSinceHold <= 8;
      if (!holding) {
         if (this.wasSpinning) {
            this.wasSpinning = false;
            this.markUpdated();
         }
      } else if (!this.stored.isEmpty()) {
         this.wasSpinning = true;
         this.angle = (this.angle + 24.0F) % 360.0F;
         this.progress += 24.0F;
         if (this.progress >= 360.0F) {
            this.progress -= 360.0F;
            this.produceOne();
         }

         float delta = Math.abs(this.angle - this.lastSyncedAngle);
         if (delta > 180.0F) {
            delta = 360.0F - delta;
         }

         if (delta >= 15.0F) {
            this.lastSyncedAngle = this.angle;
            this.markUpdated();
         }
      }
   }

   private void produceOne() {
      if (!this.stored.isEmpty() && this.level != null) {
         ItemStack product = productFor(this.stored);
         if (!product.isEmpty()) {
            this.stored.shrink(1);
            if (!this.level.isClientSide()) {
               ServerLevel serverLevel = (ServerLevel)this.level;
               serverLevel.playSound(null, this.worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6F, 1.0F);
               if (this.tryOutputToHopper(serverLevel, product)) {
                  this.markUpdated();
               } else {
                  Vec3 spawnPos = this.findPlayerDirectionSpawnPos(serverLevel);
                  ItemEntity entity = new ItemEntity(serverLevel, spawnPos.x, spawnPos.y, spawnPos.z, product.copy());
                  Vec3 outward = spawnPos.subtract(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5);
                  if (outward.lengthSqr() > 0.001) {
                     outward = outward.normalize().scale(0.12);
                     entity.setDeltaMovement(outward.x, 0.12, outward.z);
                  }

                  serverLevel.addFreshEntity(entity);
                  this.markUpdated();
               }
            }
         }
      }
   }

   private static ItemStack productFor(ItemStack input) {
      if (input.is(Items.SULFUR)) {
         return new ItemStack((ItemLike)ModItems.SULFUR_POWDER.get());
      } else {
         return input.is(Items.CINNABAR) ? new ItemStack((ItemLike)ModItems.CINNABAR_POWDER.get()) : ItemStack.EMPTY;
      }
   }

   private boolean tryOutputToHopper(ServerLevel level, ItemStack product) {
      BlockPos below = this.worldPosition.below();
      if (!(level.getBlockEntity(below) instanceof Container container)) {
         return false;
      } else {
         ItemStack remaining = product.copy();

         for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
            if (container.canPlaceItem(slot, remaining)) {
               ItemStack inSlot = container.getItem(slot);
               if (inSlot.isEmpty()) {
                  container.setItem(slot, remaining.copy());
                  remaining = ItemStack.EMPTY;
               } else if (ItemStack.isSameItemSameComponents(inSlot, remaining)) {
                  int limit = Math.min(container.getMaxStackSize(remaining), inSlot.getMaxStackSize());
                  int space = limit - inSlot.getCount();
                  if (space > 0) {
                     int moved = Math.min(space, remaining.getCount());
                     inSlot.grow(moved);
                     remaining.shrink(moved);
                     container.setItem(slot, inSlot);
                  }
               }
            }
         }

         if (remaining.getCount() != product.getCount()) {
            container.setChanged();
         }

         return remaining.isEmpty();
      }
   }

   private Vec3 findPlayerDirectionSpawnPos(ServerLevel level) {
      Player nearest = level.getNearestPlayer(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, 12.0, false);
      double cx = this.worldPosition.getX() + 0.5;
      double cy = this.worldPosition.getY() + 1.05;
      double cz = this.worldPosition.getZ() + 0.5;
      if (nearest == null) {
         return new Vec3(cx, cy, cz);
      }

      Vec3 dir = new Vec3(nearest.getX() - cx, 0.0, nearest.getZ() - cz);
      if (dir.lengthSqr() < 1.0E-4) {
         return new Vec3(cx, cy, cz);
      }

      dir = dir.normalize().scale(0.85);
      return new Vec3(cx + dir.x, cy, cz + dir.z);
   }

   private void markUpdated() {
      this.setChanged();
      if (this.level != null && !this.level.isClientSide()) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public float currentAngle() {
      return this.angle;
   }

   public void setAngleClient(float a) {
      this.angle = a;
   }

   protected void saveAdditional(ValueOutput tag) {
      super.saveAdditional(tag);
      tag.putFloat("Angle", this.angle);
      if (!this.stored.isEmpty()) {
         tag.store("Stored", ItemStack.CODEC, this.stored);
      }
   }

   protected void loadAdditional(ValueInput tag) {
      super.loadAdditional(tag);
      this.angle = tag.getFloatOr("Angle", 0.0F);
      this.lastSyncedAngle = this.angle;
      this.ticksSinceHold = Integer.MAX_VALUE;
      this.wasSpinning = false;
      this.progress = 0.0F;
      this.stored = tag.read("Stored", ItemStack.CODEC).orElse(ItemStack.EMPTY);
   }

   public CompoundTag getUpdateTag(Provider registries) {
      return this.saveCustomOnly(registries);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public void preRemoveSideEffects(BlockPos pos, BlockState state) {
      super.preRemoveSideEffects(pos, state);
      this.dropContents(this.level, pos);
   }

   public void dropContents(Level level, BlockPos pos) {
      if (!this.stored.isEmpty() && !level.isClientSide()) {
         ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, this.stored.copy());
         level.addFreshEntity(entity);
         this.stored = ItemStack.EMPTY;
      }
   }

   public boolean isFull() {
      return !this.stored.isEmpty() && this.stored.getCount() >= 64;
   }

   public boolean canSpin() {
      return !this.stored.isEmpty();
   }

   public void spinOnce() {
      this.onHoldPulse();
   }

   public static List<ItemStack> validInputs() {
      return List.of(new ItemStack(Items.SULFUR), new ItemStack(Items.CINNABAR));
   }

   public Container asContainer() {
      return null;
   }
}
