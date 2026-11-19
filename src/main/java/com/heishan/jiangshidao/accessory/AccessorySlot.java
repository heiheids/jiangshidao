package com.heishan.jiangshidao.accessory;

import com.heishan.jiangshidao.item.BronzeBellItem;
import com.heishan.jiangshidao.item.ChunyangJinItem;
import com.heishan.jiangshidao.item.QuiverItem;
import com.heishan.jiangshidao.item.TalismanPouchItem;
import net.minecraft.world.item.ItemStack;

/**
 * AccessorySlot。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public enum AccessorySlot {
   HEAD("head"),
   BACK("back"),
   WAIST("waist"),
   LEGS("legs");

   private final String id;

   AccessorySlot(String id) {
      this.id = id;
   }

   public String id() {
      return this.id;
   }

   public String translationKey() {
      return "accessory.jiangshidao." + this.id;
   }

   public boolean accepts(ItemStack stack) {
      if (stack.isEmpty()) {
         return true;
      }

      return switch (this) {
         case HEAD -> stack.getItem() instanceof ChunyangJinItem;
         case BACK -> stack.getItem() instanceof QuiverItem;
         case WAIST -> stack.getItem() instanceof TalismanPouchItem;
         case LEGS -> stack.getItem() instanceof BronzeBellItem;
      };
   }

   public static AccessorySlot byIndex(int index) {
      AccessorySlot[] values = values();
      return index >= 0 && index < values.length ? values[index] : WAIST;
   }

   public static int size() {
      return values().length;
   }
}
