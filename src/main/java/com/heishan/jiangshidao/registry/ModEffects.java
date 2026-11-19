package com.heishan.jiangshidao.registry;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.compat.RegistryHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

/**
 * ModEffects。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ModEffects {
   public static final Holder<MobEffect> SEALED = RegistryHelper.holder(
      BuiltInRegistries.MOB_EFFECT,
      "sealed",
      () -> (new MobEffect(MobEffectCategory.HARMFUL, 9127187) {})
         .addAttributeModifier(Attributes.MOVEMENT_SPEED, JiangshiDao.id("sealed_speed"), -1.0, Operation.ADD_MULTIPLIED_TOTAL)
   );
   public static final Holder<MobEffect> SUBDUED = RegistryHelper.holder(
      BuiltInRegistries.MOB_EFFECT,
      "subdued",
      () -> (new MobEffect(MobEffectCategory.NEUTRAL, 13934615) {})
         // 降服单位的移动由工人 AI 自己控制。这里**不能**把移速乘成 0：
         // 移速为 0 时原版寻路（Navigation）完全推不动实体，
         // 而耕地/半砖这类非整格方块又必须靠寻路去跨过，结果就是原地卡死。
         // 只做轻微减速，保留正常的走路能力。
         .addAttributeModifier(Attributes.MOVEMENT_SPEED, JiangshiDao.id("subdued_speed"), -0.35, Operation.ADD_MULTIPLIED_TOTAL)
   );

   public static void touch() {
   }

   private ModEffects() {
   }

   public static boolean isSubdued(LivingEntity entity) {
      return entity.hasEffect(SUBDUED);
   }

   public static boolean isSealed(LivingEntity entity) {
      return entity.hasEffect(SEALED);
   }
}
