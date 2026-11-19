package com.heishan.jiangshidao.worker;

import net.minecraft.network.chat.Component;

/**
 * BellMode。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public enum BellMode {
   FOLLOW("follow", 0),
   AGGRESSIVE("aggressive", 1),
   PASSIVE("passive", 2),
   DISABLED("disabled", 3);

   private final String id;
   private final int code;

   BellMode(String id, int code) {
      this.id = id;
      this.code = code;
   }

   public String id() {
      return this.id;
   }

   public int code() {
      return this.code;
   }

   public Component displayName() {
      return Component.translatable("bellmode.jiangshidao." + this.id);
   }

   public static BellMode byCode(int code) {
      for (BellMode mode : values()) {
         if (mode.code == code) {
            return mode;
         }
      }

      return AGGRESSIVE;
   }

   public BellMode next() {
      BellMode[] all = values();
      return all[(this.ordinal() + 1) % all.length];
   }
}
