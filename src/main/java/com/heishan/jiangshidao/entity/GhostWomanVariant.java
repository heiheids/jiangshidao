package com.heishan.jiangshidao.entity;

/**
 * GhostWomanVariant。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public enum GhostWomanVariant {
   BLUE("blue", "蓝衣女鬼", "Blue-robed Ghost Woman", 1981562, 16771102),
   RED("red", "红衣女鬼", "Red-robed Ghost Woman", 11015198, 15211291),
   WHITE("white", "白衣女鬼", "White-robed Ghost Woman", 15001320, 1315866);

   private final String id;
   private final String nameZh;
   private final String nameEn;
   private final int eggPrimary;
   private final int eggSecondary;

   GhostWomanVariant(String id, String nameZh, String nameEn, int eggPrimary, int eggSecondary) {
      this.id = id;
      this.nameZh = nameZh;
      this.nameEn = nameEn;
      this.eggPrimary = eggPrimary;
      this.eggSecondary = eggSecondary;
   }

   public String id() {
      return this.id;
   }

   public String nameZh() {
      return this.nameZh;
   }

   public String nameEn() {
      return this.nameEn;
   }

   public double powerMultiplier() {
      return switch (this) {
         case BLUE -> 1.1;
         case RED -> 1.2;
         case WHITE -> 1.0;
      };
   }

   public int eggPrimary() {
      return this.eggPrimary;
   }

   public int eggSecondary() {
      return this.eggSecondary;
   }

   public String entityPath() {
      return this == BLUE ? "ghost_woman" : "ghost_woman_" + this.id;
   }

   public String eggPath() {
      return this.entityPath() + "_spawn_egg";
   }

   public String textureFile() {
      return this == BLUE ? "ghost_woman" : "ghost_woman_" + this.id;
   }

   public String nameKey() {
      return "entity.jiangshidao." + this.entityPath();
   }

   public String eggNameKey() {
      return "item.jiangshidao." + this.eggPath();
   }
}
