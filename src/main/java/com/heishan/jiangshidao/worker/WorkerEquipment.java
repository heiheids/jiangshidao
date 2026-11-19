package com.heishan.jiangshidao.worker;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * WorkerEquipment。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class WorkerEquipment implements Container {
   public static final EquipmentSlot[] SLOTS = new EquipmentSlot[]{
      EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND
   };
   private final LivingEntity worker;

   public WorkerEquipment(LivingEntity worker) {
      this.worker = worker;
   }

   public static boolean isArmorIndex(int index) {
      return index >= 0 && index < 4;
   }

   public int getContainerSize() {
      return SLOTS.length;
   }

   public boolean isEmpty() {
      for (EquipmentSlot slot : SLOTS) {
         if (!this.worker.getItemBySlot(slot).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public ItemStack getItem(int index) {
      return index >= 0 && index < SLOTS.length ? this.worker.getItemBySlot(SLOTS[index]) : ItemStack.EMPTY;
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
      if (index >= 0 && index < SLOTS.length) {
         this.worker.setItemSlot(SLOTS[index], stack);
      }
   }

   public void setChanged() {
   }

   public boolean stillValid(Player player) {
      return true;
   }

   public void clearContent() {
      for (int i = 0; i < SLOTS.length; i++) {
         this.setItem(i, ItemStack.EMPTY);
      }
   }
}
