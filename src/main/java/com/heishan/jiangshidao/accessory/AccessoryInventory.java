package com.heishan.jiangshidao.accessory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

/**
 * AccessoryInventory。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class AccessoryInventory {
   public static final int QUIVER_SLOTS = 3;
   public static final int TOTAL_SLOTS = AccessorySlot.size() + 3;
   private final ItemStack[] slots = new ItemStack[AccessorySlot.size()];
   private final ItemStack[] quiver = new ItemStack[3];

   public AccessoryInventory() {
      for (int i = 0; i < this.slots.length; i++) {
         this.slots[i] = ItemStack.EMPTY;
      }

      for (int i = 0; i < this.quiver.length; i++) {
         this.quiver[i] = ItemStack.EMPTY;
      }
   }

   public ItemStack getQuiver(int index) {
      return index >= 0 && index < this.quiver.length ? this.quiver[index] : ItemStack.EMPTY;
   }

   public void setQuiver(int index, ItemStack stack) {
      if (index >= 0 && index < this.quiver.length) {
         this.quiver[index] = stack == null ? ItemStack.EMPTY : stack;
      }
   }

   public ItemStack get(AccessorySlot slot) {
      return this.slots[slot.ordinal()];
   }

   public void set(AccessorySlot slot, ItemStack stack) {
      this.slots[slot.ordinal()] = stack == null ? ItemStack.EMPTY : stack;
   }

   public List<ItemStack> asList() {
      List<ItemStack> list = new ArrayList<>(TOTAL_SLOTS);

      for (ItemStack stack : this.slots) {
         list.add(stack);
      }

      for (ItemStack stack : this.quiver) {
         list.add(stack);
      }

      return list;
   }

   public void setAll(List<ItemStack> list) {
      for (int i = 0; i < this.slots.length; i++) {
         this.slots[i] = i < list.size() ? list.get(i) : ItemStack.EMPTY;
      }

      for (int i = 0; i < this.quiver.length; i++) {
         int from = this.slots.length + i;
         this.quiver[i] = from < list.size() ? list.get(from) : ItemStack.EMPTY;
      }
   }

   public CompoundTag save() {
      CompoundTag tag = new CompoundTag();

      for (AccessorySlot slot : AccessorySlot.values()) {
         putStack(tag, slot.id(), this.get(slot));
      }

      for (int i = 0; i < this.quiver.length; i++) {
         putStack(tag, "quiver_" + i, this.quiver[i]);
      }

      return tag;
   }

   private static void putStack(CompoundTag tag, String key, ItemStack stack) {
      if (!stack.isEmpty()) {
         ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack).result().ifPresent(t -> tag.put(key, t));
      }
   }

   private static ItemStack readStack(CompoundTag tag, String key) {
      return !tag.contains(key)
         ? ItemStack.EMPTY
         : ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, tag.getCompoundOrEmpty(key)).result().orElse(ItemStack.EMPTY);
   }

   public static AccessoryInventory load(CompoundTag tag) {
      AccessoryInventory inv = new AccessoryInventory();

      for (AccessorySlot slot : AccessorySlot.values()) {
         inv.set(slot, readStack(tag, slot.id()));
      }

      for (int i = 0; i < 3; i++) {
         inv.setQuiver(i, readStack(tag, "quiver_" + i));
      }

      return inv;
   }
}
