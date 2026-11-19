package com.heishan.jiangshidao.menu;

import com.heishan.jiangshidao.accessory.AccessoryInventory;
import com.heishan.jiangshidao.accessory.AccessorySlot;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * AccessoryContainer。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class AccessoryContainer implements Container {
   private final AccessoryInventory inventory;
   private Runnable changeListener = () -> {};

   public AccessoryContainer(AccessoryInventory inventory) {
      this.inventory = inventory;
   }

   public AccessoryInventory inventory() {
      return this.inventory;
   }

   public int getContainerSize() {
      return AccessorySlot.size();
   }

   public boolean isEmpty() {
      for (AccessorySlot slot : AccessorySlot.values()) {
         if (!this.inventory.get(slot).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public ItemStack getItem(int index) {
      return this.inventory.get(AccessorySlot.byIndex(index));
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
      this.inventory.set(AccessorySlot.byIndex(index), stack);
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
      for (AccessorySlot slot : AccessorySlot.values()) {
         this.inventory.set(slot, ItemStack.EMPTY);
      }
   }
}
