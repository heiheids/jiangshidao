package com.heishan.jiangshidao.worldgen;

import com.heishan.jiangshidao.block.PeachTreeGrower;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * PeachTreeFeature。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class PeachTreeFeature implements Feature {
   public static final MapCodec<PeachTreeFeature> CODEC = MapCodec.unit(new PeachTreeFeature());

   public MapCodec<? extends Feature> codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      return PeachTreeGrower.grow(level, origin, random, false);
   }
}
