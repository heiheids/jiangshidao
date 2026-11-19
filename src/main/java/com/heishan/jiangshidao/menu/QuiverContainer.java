package com.heishan.jiangshidao.menu;

import com.heishan.jiangshidao.accessory.AccessoryInventory;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * QuiverContainer。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class QuiverContainer implements Container {
   private final AccessoryInventory inventory;
   private Runnable changeListener = () -> {};

   public QuiverContainer(AccessoryInventory inventory) {
      this.inventory = inventory;
   }

   public int getContainerSize() {
      return 3;
   }

   public boolean isEmpty() {
      for (int i = 0; i < this.getContainerSize(); i++) {
         if (!this.inventory.getQuiver(i).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public ItemStack getItem(int index) {
      return this.inventory.getQuiver(index);
   }

   public ItemStack removeItem(int index, int count) {
      ItemStack current = this.getItem(index);
      if (current.isEmpty()) {
         return ItemStack.EMPTY;
      }

      ItemStack taken = current.split(count);
      if (current.isEmpty()) {
         this.setItem(index, ItemStack.EMPTY);
      }

      return taken;
   }

   public ItemStack removeItemNoUpdate(int index) {
      ItemStack current = this.getItem(index);
      this.setItem(index, ItemStack.EMPTY);
      return current;
   }

   public void setItem(int index, ItemStack stack) {
      this.inventory.setQuiver(index, stack);
   }

   public void setChangeListener(Runnable listener) {
      this.changeListener = listener == null ? () -> {} : listener;
   }

   public void setChanged() {
      this.changeListener.run();
   }

   public boolean stillValid(Player player) {
      return true;
   }

   public void clearContent() {
      for (int i = 0; i < this.getContainerSize(); i++) {
         this.inventory.setQuiver(i, ItemStack.EMPTY);
      }
   }
}
