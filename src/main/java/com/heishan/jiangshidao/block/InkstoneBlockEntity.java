package com.heishan.jiangshidao.block;

import com.heishan.jiangshidao.registry.ModBlockEntities;
import com.heishan.jiangshidao.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * InkstoneBlockEntity —— 方块实体，负责该方块的状态与刻逻辑。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class InkstoneBlockEntity extends BlockEntity {
   public static final int MAX_CINNABAR = 64;
   private int cinnabarCount;
   private boolean hasWater;

   public InkstoneBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.INKSTONE.get(), pos, state);
   }

   public int cinnabarCount() {
      return this.cinnabarCount;
   }

   public boolean hasWater() {
      return this.hasWater;
   }

   public boolean canGrind() {
      return this.hasWater && this.cinnabarCount > 0;
   }

   public int remainingCinnabarCapacity() {
      return Math.max(0, 64 - this.cinnabarCount);
   }

   public boolean isCinnabarFull() {
      return this.cinnabarCount >= 64;
   }

   public boolean addCinnabar() {
      if (this.isCinnabarFull()) {
         return false;
      }

      this.cinnabarCount++;
      this.markUpdated();
      return true;
   }

   public boolean addWater() {
      if (this.hasWater) {
         return false;
      }

      this.hasWater = true;
      this.markUpdated();
      return true;
   }

   public void consumeOneCinnabar() {
      if (this.cinnabarCount > 0) {
         this.cinnabarCount--;
         this.markUpdated();
      }
   }

   public int takeAllCinnabar() {
      int n = this.cinnabarCount;
      this.cinnabarCount = 0;
      this.hasWater = false;
      this.markUpdated();
      return n;
   }

   public void preRemoveSideEffects(BlockPos pos, BlockState state) {
      super.preRemoveSideEffects(pos, state);
      this.dropCinnabar(this.level, pos);
   }

   public void dropCinnabar(Level level, BlockPos pos) {
      if (level != null && !level.isClientSide()) {
         int count = this.takeAllCinnabar();
         if (count > 0) {
            ItemStack drop = new ItemStack((ItemLike)ModItems.CINNABAR_POWDER.get(), count);
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, drop);
            level.addFreshEntity(entity);
         }
      }
   }

   private void markUpdated() {
      this.setChanged();
      if (this.level != null && !this.level.isClientSide()) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   protected void saveAdditional(ValueOutput tag) {
      super.saveAdditional(tag);
      tag.putInt("Cinnabar", this.cinnabarCount);
      tag.putBoolean("Water", this.hasWater);
   }

   protected void loadAdditional(ValueInput tag) {
      super.loadAdditional(tag);
      this.cinnabarCount = tag.getIntOr("Cinnabar", 0);
      this.hasWater = tag.getBooleanOr("Water", false);
   }

   public CompoundTag getUpdateTag(Provider registries) {
      return this.saveCustomOnly(registries);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public String statusText() {
      String water = this.hasWater ? "§b有水" : "§7无水";
      String cin = this.cinnabarCount > 0 ? "§c朱砂 " + this.cinnabarCount + "/64" : "§7朱砂 0/64";
      return water + "§7 | " + cin;
   }

   public static InkstoneBlockEntity at(Level level, BlockPos pos) {
      if (level == null) {
         return null;
      } else {
         return level.getBlockEntity(pos) instanceof InkstoneBlockEntity ink ? ink : null;
      }
   }
}
