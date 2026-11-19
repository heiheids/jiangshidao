package com.heishan.jiangshidao.entity;

/**
 * JiangshiType。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public enum JiangshiType {
   TEAL("teal", "石青僵尸", "Teal Jiangshi", 40.0, 3.5999999999999996, 0.23, false, JiangshiType.JiangshiModelType.NORMAL),
   PURPLE("purple", "紫袍僵尸", "Purple Jiangshi", 60.0, 4.5, 0.276, false, JiangshiType.JiangshiModelType.NORMAL),
   YELLOW("yellow", "黄袍僵尸", "Yellow Jiangshi", 80.0, 5.4, 0.34500000000000003, true, JiangshiType.JiangshiModelType.NORMAL),
   BRUTE("brute", "魁梧僵尸", "Brute Jiangshi", 200.0, 9.0, 0.18400000000000002, true, JiangshiType.JiangshiModelType.BRUTE);

   private final String id;
   private final String nameZh;
   private final String nameEn;
   private final double maxHealth;
   private final double attackDamage;
   private final double movementSpeed;
   private final boolean sunImmune;
   private final JiangshiType.JiangshiModelType modelType;

   JiangshiType(
      String id,
      String nameZh,
      String nameEn,
      double maxHealth,
      double attackDamage,
      double movementSpeed,
      boolean sunImmune,
      JiangshiType.JiangshiModelType modelType
   ) {
      this.id = id;
      this.nameZh = nameZh;
      this.nameEn = nameEn;
      this.maxHealth = maxHealth;
      this.attackDamage = attackDamage;
      this.movementSpeed = movementSpeed;
      this.sunImmune = sunImmune;
      this.modelType = modelType;
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

   public double maxHealth() {
      return this.maxHealth;
   }

   public double attackDamage() {
      return this.attackDamage;
   }

   public double movementSpeed() {
      return this.movementSpeed;
   }

   public boolean sunImmune() {
      return this.sunImmune;
   }

   public JiangshiType.JiangshiModelType modelType() {
      return this.modelType;
   }

   public String entityPath() {
      return this.id + "_jiangshi";
   }

   public String eggPath() {
      return this.id + "_jiangshi_spawn_egg";
   }

   public String nameKey() {
      return "entity.jiangshidao." + this.entityPath();
   }

   public String eggNameKey() {
      return "item.jiangshidao." + this.eggPath();
   }

   public enum JiangshiModelType {
      NORMAL,
      BRUTE;
   }
}
