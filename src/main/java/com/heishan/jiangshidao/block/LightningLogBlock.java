package com.heishan.jiangshidao.block;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.redstone.Orientation;

/**
 * LightningLogBlock —— 方块定义与交互行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class LightningLogBlock extends RotatedPillarBlock {
   public static final int BURN_TICKS = 120;
   public static final float SURVIVE_CHANCE = 0.1F;
   public static final float UPGRADE_CHANCE = 0.01F;
   public static final int LIGHTNING_IGNITE_RADIUS = 6;
   private final LightningLogBlock.BurnProfile profile;

   public LightningLogBlock(Properties properties, LightningLogBlock.BurnProfile profile) {
      super(properties);
      this.profile = profile;
   }

   public LightningLogBlock.BurnProfile profile() {
      return this.profile;
   }

   public boolean ignite(ServerLevel level, BlockPos pos, BlockState state) {
      if (this.profile == null) {
         return false;
      }

      if (level.getBlockTicks().hasScheduledTick(pos, this)) {
         return false;
      }

      level.scheduleTick(pos, this, 120);
      BlockPos above = pos.above();
      if (level.isEmptyBlock(above)) {
         level.setBlockAndUpdate(above, BaseFireBlock.getState(level, above));
      }

      return true;
   }

   public static int igniteNearby(ServerLevel level, BlockPos center) {
      int ignited = 0;
      int r = 6;

      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
         BlockState state = level.getBlockState(pos);
         if (state.getBlock() instanceof LightningLogBlock log && log.ignite(level, pos.immutable(), state)) {
            ignited++;
         }
      }

      return ignited;
   }

   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      this.resolveBurn(level, pos, state, random.nextFloat(), random.nextFloat());
   }

   public void resolveBurn(ServerLevel level, BlockPos pos, BlockState state, float surviveRoll, float upgradeRoll) {
      if (this.profile != null) {
         clearFireAbove(level, pos);
         if (surviveRoll >= this.profile.surviveChance()) {
            level.removeBlock(pos, false);
         } else {
            Block next = this.profile.survivor().get();
            if (this.profile.upgraded() != null && upgradeRoll < this.profile.upgradeChance()) {
               next = this.profile.upgraded().get();
            }

            if (next != this) {
               level.setBlockAndUpdate(pos, withSameAxis(state, next));
            }
         }
      }
   }

   private static void clearFireAbove(ServerLevel level, BlockPos pos) {
      BlockPos above = pos.above();
      if (level.getBlockState(above).is(Blocks.FIRE)) {
         level.removeBlock(above, false);
      }
   }

   private static BlockState withSameAxis(BlockState from, Block to) {
      BlockState result = to.defaultBlockState();
      if (result.hasProperty(AXIS) && from.hasProperty(AXIS)) {
         result = (BlockState)result.setValue(AXIS, (Axis)from.getValue(AXIS));
      }

      return result;
   }

   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, Orientation orientation, boolean movedByPiston) {
      super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
      if (level instanceof ServerLevel serverLevel && hasAdjacentFire(level, pos)) {
         this.ignite(serverLevel, pos, state);
      }
   }

   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (hasAdjacentFire(level, pos)) {
         this.ignite(level, pos, state);
      }
   }

   private static boolean hasAdjacentFire(Level level, BlockPos pos) {
      for (Direction direction : Direction.values()) {
         if (level.getBlockState(pos.relative(direction)).is(Blocks.FIRE)) {
            return true;
         }
      }

      return level.getBlockState(pos.above()).is(Blocks.FIRE);
   }

   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if (level.getBlockState(pos.above()).is(Blocks.FIRE)) {
         for (int i = 0; i < 2; i++) {
            level.addParticle(
               ParticleTypes.LARGE_SMOKE,
               pos.getX() + 0.15 + random.nextDouble() * 0.7,
               pos.getY() + 1.0,
               pos.getZ() + 0.15 + random.nextDouble() * 0.7,
               0.0,
               0.02,
               0.0
            );
         }
      }
   }

   public record BurnProfile(float surviveChance, Supplier<? extends Block> survivor, float upgradeChance, Supplier<? extends Block> upgraded) {
   }
}
