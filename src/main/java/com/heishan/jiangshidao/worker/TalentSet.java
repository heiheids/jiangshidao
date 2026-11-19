package com.heishan.jiangshidao.worker;

import com.heishan.jiangshidao.JiangshiDao;

import com.heishan.jiangshidao.compat.PersistentData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

/**
 * TalentSet —— 数据模型。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class TalentSet {
   private static final String KEY = "JiangshiTalents";
   private static final String MOD_HEALTH = "jiangshidao_talent_health";
   private static final String MOD_ATTACK = "jiangshidao_talent_attack";
   private static final String MOD_SPEED = "jiangshidao_talent_speed";
   private static final Map<LivingEntity, TalentSet> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
   private final LivingEntity entity;
   private final Map<Talent, Integer> levels = new EnumMap<>(Talent.class);
   private boolean applied;

   private TalentSet(LivingEntity entity) {
      this.entity = entity;
      this.load();
   }

   public static TalentSet of(LivingEntity entity) {
      return CACHE.computeIfAbsent(entity, TalentSet::new);
   }

   public static void ensureAssigned(LivingEntity entity) {
      TalentSet set = of(entity);
      if (set.levels.isEmpty()) {
         set.roll();
         set.applyAttributeModifiers();
      } else {
         if (!set.applied) {
            set.applyAttributeModifiers();
         }
      }
   }

   private void load() {
      CompoundTag data = PersistentData.of(this.entity);
      if (data.contains("JiangshiTalents")) {
         CompoundTag talents = data.getCompoundOrEmpty("JiangshiTalents");

         for (String key : talents.keySet()) {
            Talent talent = Talent.byId(key);
            if (talent != null) {
               this.levels.put(talent, talents.getIntOr(key, 0));
            }
         }
      }
   }

   private void save() {
      CompoundTag talents = new CompoundTag();
      this.levels.forEach((talent, level) -> talents.putInt(talent.id(), level));
      PersistentData.of(this.entity).put("JiangshiTalents", talents);
      PersistentData.flush(this.entity);
   }

   public static void restoreFromDescription(LivingEntity entity, CompoundTag described) {
      if (described != null && !described.isEmpty()) {
         CompoundTag talents = new CompoundTag();

         for (Talent talent : Talent.values()) {
            CompoundTag row = described.getCompoundOrEmpty(talent.id());
            int level = row.getIntOr("lv", 0);
            if (level > 0) {
               talents.putInt(talent.id(), level);
            }
         }

         if (!talents.isEmpty()) {
            CompoundTag data = PersistentData.of(entity);
            if (!data.contains("JiangshiTalents")) {
               data.put("JiangshiTalents", talents);
               PersistentData.flush(entity);
            }
         }
      }
   }

   private void roll() {
      RandomSource random = this.entity.getRandom();

      for (Talent talent : Talent.values()) {
         this.levels.put(talent, rollLevel(random));
      }

      this.save();
   }

   private static int rollLevel(RandomSource random) {
      int roll = random.nextInt(100);
      if (roll < 40) {
         return 1;
      } else if (roll < 65) {
         return 2;
      } else if (roll < 83) {
         return 3;
      } else {
         return roll < 94 ? 4 : 5;
      }
   }

   public int level(Talent talent) {
      return this.levels.getOrDefault(talent, 0);
   }

   public boolean has(Talent talent) {
      return this.levels.containsKey(talent);
   }

   public double multiplier(Talent talent) {
      int level = this.level(talent);
      return level <= 0 ? 1.0 : talent.multiplier(level);
   }

   public static CompoundTag describe(LivingEntity entity) {
      TalentSet set = of(entity);
      CompoundTag out = new CompoundTag();
      double health = attribute(entity, Attributes.MAX_HEALTH);
      double attack = attribute(entity, Attributes.ATTACK_DAMAGE);
      double speed = attribute(entity, Attributes.MOVEMENT_SPEED);
      double defense = set.defenseMultiplier();
      double reduction = (1.0 - 1.0 / Math.max(1.0, defense)) * 100.0;

      for (Talent talent : Talent.values()) {
         CompoundTag row = new CompoundTag();
         row.putInt("lv", set.level(talent));

         String value = switch (talent) {
            case HEALTH -> String.format("%.1f", health);
            case ATTACK -> String.format("%.1f", attack);
            case SPEED -> String.format("%.2f", baseAttribute(entity, Attributes.MOVEMENT_SPEED) * set.movementMultiplier());
            case DEFENSE -> String.format("%.0f%%", reduction);
            case WAR -> String.format("%.0f%%", set.multiplier(Talent.WAR) * 100.0);
            default -> String.format("%.0f%%", set.multiplier(talent) * 100.0);
         };
         row.putString("v", value);
         out.put(talent.id(), row);
      }

      return out;
   }

   private static double attribute(LivingEntity entity, Holder<Attribute> attribute) {
      AttributeInstance instance = entity.getAttribute(attribute);
      return instance == null ? 0.0 : instance.getValue();
   }

   private static double baseAttribute(LivingEntity entity, Holder<Attribute> attribute) {
      AttributeInstance instance = entity.getAttribute(attribute);
      return instance == null ? 0.0 : instance.getBaseValue();
   }

   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      this.levels.forEach((talent, level) -> tag.putInt(talent.id(), level));
      return tag;
   }

   public static List<Entry<Talent, Integer>> parse(CompoundTag tag) {
      List<Entry<Talent, Integer>> list = new ArrayList<>();
      if (tag == null) {
         return list;
      }

      for (Talent talent : Talent.values()) {
         if (tag.contains(talent.id())) {
            list.add(Map.entry(talent, tag.getIntOr(talent.id(), 0)));
         }
      }

      return list;
   }

   public List<Entry<Talent, Integer>> entries() {
      return new ArrayList<>(this.levels.entrySet());
   }

   public boolean isEmpty() {
      return this.levels.isEmpty();
   }

   public void applyAttributeModifiers() {
      double health = this.multiplier(Talent.HEALTH);
      double attack = this.multiplier(Talent.ATTACK) * this.multiplier(Talent.WAR);
      double speed = this.multiplier(Talent.SPEED) * this.multiplier(Talent.WAR);
      apply(this.entity, Attributes.MAX_HEALTH, "jiangshidao_talent_health", health);
      apply(this.entity, Attributes.ATTACK_DAMAGE, "jiangshidao_talent_attack", attack);
      apply(this.entity, Attributes.MOVEMENT_SPEED, "jiangshidao_talent_speed", speed);
      this.applied = true;
   }

   private static void apply(LivingEntity entity, Holder<Attribute> attribute, String id, double multiplier) {
      AttributeInstance instance = entity.getAttribute(attribute);
      if (instance != null) {
         Identifier key = JiangshiDao.id(id);
         instance.removeModifier(key);
         double amount = multiplier - 1.0;
         if (!(Math.abs(amount) < 1.0E-4)) {
            instance.addPermanentModifier(new AttributeModifier(key, amount, Operation.ADD_MULTIPLIED_TOTAL));
         }
      }
   }

   public double defenseMultiplier() {
      return this.multiplier(Talent.DEFENSE) * this.multiplier(Talent.WAR);
   }

   public double movementMultiplier() {
      return this.multiplier(Talent.SPEED) * this.multiplier(Talent.WAR);
   }

   public double workSpeedMultiplier(WorkerJob job) {
      double value = this.multiplier(Talent.SPEED);

      for (Talent talent : Talent.values()) {
         if (talent.isWorkTalent() && talent.matchesJob(job)) {
            value *= this.multiplier(talent);
            break;
         }
      }

      return Math.min(3.0, value);
   }

   public int jobTalentLevel(WorkerJob job) {
      for (Talent talent : Talent.values()) {
         if (talent.isWorkTalent() && talent.matchesJob(job)) {
            return this.level(talent);
         }
      }

      return 0;
   }
}
