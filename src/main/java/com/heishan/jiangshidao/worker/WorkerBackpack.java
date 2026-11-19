package com.heishan.jiangshidao.worker;

import com.heishan.jiangshidao.compat.PersistentData;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * WorkerBackpack。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class WorkerBackpack implements Container {
   public static final int SIZE = 27;
   private static final String KEY = "JiangshiWorkerBackpack";
   private final LivingEntity worker;
   private final Provider registries;
   private final ItemStack[] items = new ItemStack[27];

   public WorkerBackpack(LivingEntity worker, Provider registries) {
      this.worker = worker;
      this.registries = registries;

      for (int i = 0; i < 27; i++) {
         this.items[i] = ItemStack.EMPTY;
      }

      this.load();
   }

   private void load() {
      CompoundTag data = PersistentData.of(this.worker);
      if (data.contains("JiangshiWorkerBackpack")) {
         ListTag list = data.getListOrEmpty("JiangshiWorkerBackpack");

         for (int i = 0; i < Math.min(27, list.size()); i++) {
            this.items[i] = ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, list.getCompoundOrEmpty(i)).result().orElse(ItemStack.EMPTY);
         }
      }
   }

   private void save() {
      ListTag list = new ListTag();

      for (ItemStack stack : this.items) {
         if (stack.isEmpty()) {
            list.add(new CompoundTag());
         } else {
            list.add((Tag)ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack).result().orElseGet(CompoundTag::new));
         }
      }

      PersistentData.of(this.worker).put("JiangshiWorkerBackpack", list);
   }

   public int getContainerSize() {
      return 27;
   }

   public boolean isEmpty() {
      for (ItemStack stack : this.items) {
         if (!stack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public ItemStack getItem(int index) {
      return index >= 0 && index < 27 ? this.items[index] : ItemStack.EMPTY;
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
      if (index >= 0 && index < 27) {
         this.items[index] = stack;
         this.save();
      }
   }

   public void setChanged() {
      this.save();
   }

   public boolean stillValid(Player player) {
      return true;
   }

   public void clearContent() {
      for (int i = 0; i < 27; i++) {
         this.items[i] = ItemStack.EMPTY;
      }

      this.save();
   }
}
