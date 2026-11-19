package com.heishan.jiangshidao.worker;

/**
 * Talent —— 数据模型。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public enum Talent {
   HEALTH("health", 0.8, 0.975, 1.15, 1.325, 1.5),
   SPEED("speed", 0.8, 0.975, 1.15, 1.325, 1.5),
   ATTACK("attack", 0.8, 0.975, 1.15, 1.325, 1.5),
   DEFENSE("defense", 1.0, 1.25, 1.5, 1.75, 2.0),
   FARMING("farming", 1.0, 1.2, 1.4, 1.6, 2.0),
   MINING("mining", 1.0, 1.25, 1.5, 1.75, 2.0),
   FISHING("fishing", 1.0, 1.25, 1.5, 1.75, 2.0),
   WOODCUTTING("woodcutting", 1.0, 1.25, 1.5, 1.75, 2.0),
   WAR("war", 1.0, 1.25, 1.5, 1.75, 2.0);

   public static final int MAX_LEVEL = 5;
   public static final int ROLL_COUNT = 3;
   private final String id;
   private final double[] multipliers;

   Talent(String id, double... multipliers) {
      this.id = id;
      this.multipliers = multipliers;
   }

   public String id() {
      return this.id;
   }

   public String translationKey() {
      return "talent.jiangshidao." + this.id;
   }

   public double multiplier(int level) {
      int index = Math.max(1, Math.min(5, level)) - 1;
      return this.multipliers[index];
   }

   public boolean isWorkTalent() {
      return this == FARMING || this == MINING || this == FISHING || this == WOODCUTTING;
   }

   public boolean matchesJob(WorkerJob job) {
      return switch (this) {
         case FARMING -> job == WorkerJob.FARMING || job == WorkerJob.PLANTING;
         case MINING -> job == WorkerJob.MINING;
         case FISHING -> job == WorkerJob.FISHING;
         case WOODCUTTING -> job == WorkerJob.WOODCUTTING;
         default -> false;
      };
   }

   public static Talent byId(String id) {
      for (Talent talent : values()) {
         if (talent.id.equals(id)) {
            return talent;
         }
      }

      return null;
   }

   public static String stars(int level) {
      int filled = Math.max(0, Math.min(5, level));
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < 5; i++) {
         sb.append((char)(i < filled ? '★' : '☆'));
      }

      return sb.toString();
   }
}
