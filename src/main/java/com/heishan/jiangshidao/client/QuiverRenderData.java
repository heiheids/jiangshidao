package com.heishan.jiangshidao.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;

/**
 * QuiverRenderData —— 数据模型。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class QuiverRenderData {
   public static final int SHEATHS = 3;
   public final ItemStackRenderState quiver = new ItemStackRenderState();
   public final ItemStackRenderState[] swords = new ItemStackRenderState[3];
   public final ItemStack[] swordStacks = new ItemStack[3];

   public QuiverRenderData() {
      for (int i = 0; i < 3; i++) {
         this.swords[i] = new ItemStackRenderState();
         this.swordStacks[i] = ItemStack.EMPTY;
      }
   }

   public boolean hasSword(int index) {
      return index >= 0 && index < 3 && !this.swords[index].isEmpty();
   }
}
