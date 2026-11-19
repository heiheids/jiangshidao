package com.heishan.jiangshidao.worker;

import net.minecraft.network.chat.Component;

/**
 * WorkerJob。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public enum WorkerJob {
   IDLE("idle", false),
   FARMING("farming", true),
   FISHING("fishing", true),
   MINING("mining", true),
   WOODCUTTING("woodcutting", true),
   PLANTING("planting", true);

   private final String id;
   private final boolean implemented;

   WorkerJob(String id, boolean implemented) {
      this.id = id;
      this.implemented = implemented;
   }

   public String id() {
      return this.id;
   }

   public boolean implemented() {
      return this.implemented;
   }

   public String translationKey() {
      return "job.jiangshidao." + this.id;
   }

   public Component displayName() {
      return Component.translatable(this.translationKey());
   }

   public static WorkerJob byId(String id) {
      for (WorkerJob job : values()) {
         if (job.id.equals(id)) {
            return job;
         }
      }

      return IDLE;
   }
}
