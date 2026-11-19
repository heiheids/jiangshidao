package com.heishan.jiangshidao.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

/**
 * PeachSaplingBlock —— 方块定义与交互行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class PeachSaplingBlock extends BushBlock implements BonemealableBlock {
   public PeachSaplingBlock(Properties properties) {
      super(properties);
   }

   protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
      // BlockTags.DIRT 只含泥土/砂土/缠根泥土，不含草方块等。
      // 原版树苗用的是"可以长植物的地面"判断，这里对齐原版：
      // 草方块、泥土、peat、farmland、菌丝、苔藓等都能种。
      return state.is(BlockTags.DIRT)
         || state.is(Blocks.GRASS_BLOCK)
         || state.is(Blocks.PODZOL)
         || state.is(Blocks.MYCELIUM)
         || state.is(Blocks.MOSS_BLOCK)
         || state.is(Blocks.FARMLAND)
         || state.is(Blocks.ROOTED_DIRT);
   }

   public boolean isRandomlyTicking(BlockState state) {
      return true;
   }

   public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(7) == 0) {
         this.grow(level, pos, random);
      }
   }

   public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
      // 只有当上方真的有足够空间长树时，才允许骨粉作用。
      // 否则原版会先消耗骨粉、再调用 performBonemeal，而 grow() 因空间不足
      // 返回 false —— 玩家看到的就是"骨粉用掉了、树苗没了"。
      return PeachTreeGrower.hasRoomToGrow(level, pos);
   }

   public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
      // 空间足够时必定成树：树木生长不像作物那样分阶段，
      // 返回随机成功率会让玩家反复浪费骨粉。
      return true;
   }

   public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
      this.grow(level, pos, random);
   }

   public boolean grow(ServerLevel level, BlockPos pos, RandomSource random) {
      return PeachTreeGrower.grow(level, pos, random);
   }
}
