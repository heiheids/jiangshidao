package com.heishan.jiangshidao.client;

/**
 * PouchSelection。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class PouchSelection {
   private static int selected = -1;

   private PouchSelection() {
   }

   public static int selected() {
      return selected;
   }

   public static void reset() {
      selected = -1;
   }

   public static void cycle(int delta, int slotCount) {
      if (slotCount <= 0) {
         selected = -1;
      } else if (selected < 0) {
         selected = delta > 0 ? 0 : slotCount - 1;
      } else {
         selected = ((selected + delta) % slotCount + slotCount) % slotCount;
      }
   }

   public static void set(int index, int slotCount) {
      if (index >= 0 && index < slotCount) {
         selected = index;
      } else {
         selected = -1;
      }
   }
}
