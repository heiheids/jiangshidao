package com.heishan.jiangshidao.registry;

import com.heishan.jiangshidao.compat.Reg;
import com.heishan.jiangshidao.compat.RegistryHelper;
import com.heishan.jiangshidao.worldgen.PeachTreeFeature;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * ModFeatures。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ModFeatures {
   public static final Reg<MapCodec<? extends Feature>> PEACH_TREE = RegistryHelper.reg(
      BuiltInRegistries.FEATURE_TYPE, "peach_tree", () -> PeachTreeFeature.CODEC
   );

   public static void touch() {
   }

   private ModFeatures() {
   }
}
