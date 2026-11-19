package com.heishan.jiangshidao.block;

import com.heishan.jiangshidao.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * PeachTreeGrower。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class PeachTreeGrower {
   /** 树干最大高度（4 + nextInt(3) 的上限）。 */
   private static final int MAX_HEIGHT = 6;

   private PeachTreeGrower() {
   }

   /**
    * 检查该位置上方是否有足够空间长成桃树。
    *
    * <p>供 {@code isValidBonemealTarget} 使用：空间不足时就不该允许骨粉作用，
    * 否则骨粉会被消耗、树却长不出来。
    */
   public static boolean hasRoomToGrow(LevelReader level, BlockPos pos) {
      // 与 grow() 的最大树干高度保持一致（4 + nextInt(3) 的上限是 6）。
      for (int dy = 1; dy <= MAX_HEIGHT + 1; dy++) {
         if (!level.isEmptyBlock(pos.above(dy))) {
            return false;
         }
      }

      return true;
   }

   public static boolean grow(LevelAccessor level, BlockPos pos, RandomSource random) {
      return grow(level, pos, random, true);
   }

   public static boolean grow(LevelAccessor level, BlockPos pos, RandomSource random, boolean requireGround) {
      if (requireGround && !isGround(level.getBlockState(pos.below()))) {
         return false;
      }

      int trunkHeight = 4 + random.nextInt(3);

      for (int dy = 1; dy <= trunkHeight + 1; dy++) {
         if (!level.isEmptyBlock(pos.above(dy))) {
            return false;
         }
      }

      BlockState log = ModBlocks.PEACH_LOG.get().defaultBlockState();
      BlockState leaves = ModBlocks.PEACH_LEAVES.get().defaultBlockState();

      // 第一段原木正好放在树苗所在位置（原版树苗长树也是这样：树苗被原木顶替）。
      // 用 flag 3（BLOCK_UPDATE | SEND_TO_CLIENT）而不是 2：
      // flag 2 只更新客户端、不触发邻居更新，方块状态与实际不同步，
      // 树苗会被"看不见地"覆盖掉，表现为骨粉一用树苗就没了。
      for (int dy = 0; dy < trunkHeight; dy++) {
         level.setBlock(pos.above(dy), log, 3);
      }

      BlockPos top = pos.above(trunkHeight);

      for (int dy = -2; dy <= 1; dy++) {
         int radius = dy <= -1 ? 2 : 1;

         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if (Math.abs(dx) != radius || Math.abs(dz) != radius || !random.nextBoolean()) {
                  BlockPos target = top.offset(dx, dy, dz);
                  if (level.isEmptyBlock(target)) {
                     level.setBlock(target, leaves, 3);
                  }
               }
            }
         }
      }

      return true;
   }

   private static boolean isGround(BlockState state) {
      return state.is(BlockTags.DIRT)
         || state.is(Blocks.GRASS_BLOCK)
         || state.is(Blocks.PODZOL)
         || state.is(Blocks.MYCELIUM)
         || state.is(Blocks.MOSS_BLOCK)
         || state.is(Blocks.FARMLAND);
   }
}
