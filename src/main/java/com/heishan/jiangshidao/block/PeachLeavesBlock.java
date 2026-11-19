package com.heishan.jiangshidao.block;

import com.heishan.jiangshidao.registry.ModBlocks;
import com.heishan.jiangshidao.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 桃树叶。
 *
 * <p>自然生长的桃树叶（有树干相连、不是玩家放置的）会在随机刻尝试结果：
 * <ul>
 *   <li>必须有至少一个面暴露在空气中（六面中任意一个相邻格为空气）</li>
 *   <li>光照足够（与浆果丛一致：亮度 ≥ 9）</li>
 *   <li>每次随机刻约 1/5 概率结果，节奏参考浆果丛</li>
 * </ul>
 *
 * <p>成熟的桃子通过 {@link #HAS_PEACH} 方块状态表示，模型会在叶片表面画出桃子；
 * 玩家空手或持物右键即可摘下，得到一个桃子。
 *
 * <p>判断"自然生长"用原版已有的两个属性：
 * {@code PERSISTENT=false}（不是玩家放置）且 {@code DISTANCE < 7}（与树干相连）。
 * 因此玩家自己摆的桃叶、以及没有树干支撑的悬浮桃叶都不会结果。
 *
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class PeachLeavesBlock extends LeavesBlock {
   /** 是否已结出成熟桃子。 */
   public static final BooleanProperty HAS_PEACH = BooleanProperty.create("has_peach");
   /** 结果所需的随机刻概率分母（1/5，与浆果丛同节奏）。 */
   private static final int GROW_CHANCE = 5;
   /** 结果所需最低光照。 */
   private static final int MIN_LIGHT = 9;

   /**
    * 是否需要随机刻。
    *
    * <p><b>这是桃子能长出来的关键。</b>原版 {@code LeavesBlock.isRandomlyTicking}
    * 只在 {@code DISTANCE == 7}（已与树干断开、即将腐烂）且非玩家放置时返回 true ——
    * 也就是说正常长在树上的叶子<b>永远不会触发随机刻</b>，
    * 我之前把结果逻辑放在 randomTick 里，等于永远不执行。
    *
    * <p>这里改为：自然树叶（PERSISTENT=false）一律参与随机刻，
    * 既能让桃子结果，父类的腐烂逻辑也照常工作（它在 randomTick 内部按 DISTANCE 判断）。
    * 玩家放置的叶子（PERSISTENT=true）不需要腐烂也不需要结果，直接跳过以省性能。
    */
   public boolean isRandomlyTicking(BlockState state) {
      return !state.getValue(PERSISTENT);
   }

   public PeachLeavesBlock(AmbientLeavesBlockSoundPlayer soundPlayer, Properties properties) {
      super(soundPlayer, properties);
      // 必须基于父类已有的默认状态再追加 HAS_PEACH。
      // 若直接用 stateDefinition.any() 重建，会丢掉父类设定的
      // DISTANCE=7 / PERSISTENT=false / WATERLOGGED=false，
      // 导致默认状态变成"含水"且距离为 0（表现为树叶含水、永不腐烂）。
      this.registerDefaultState(this.defaultBlockState().setValue(HAS_PEACH, Boolean.FALSE));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(HAS_PEACH);
   }

   public void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource random) {
      // 先交给父类处理落叶/腐烂逻辑
      super.randomTick(state, level, pos, random);

      if (state.getValue(HAS_PEACH)) {
         return;
      }

      if (!canBearFruit(state)) {
         return;
      }

      if (level.getMaxLocalRawBrightness(pos) < MIN_LIGHT) {
         return;
      }

      if (!hasExposedFace(level, pos)) {
         return;
      }

      if (random.nextInt(GROW_CHANCE) == 0) {
         level.setBlock(pos, state.setValue(HAS_PEACH, Boolean.TRUE), 3);
      }
   }

   /**
    * 是否可以结果：必须是与树干相连的自然树叶。
    *
    * <p>{@code PERSISTENT=true} 表示这格树叶是玩家放置的，不结果；
    * {@code DISTANCE=7} 表示已与树干断开，同样不结果。
    */
   private static boolean canBearFruit(BlockState state) {
      // 只排除玩家放置的叶子。不与树干相连的叶子会被父类的腐烂逻辑移除，
      // 不需要在这里用 DISTANCE 判定（那个条件在正常树上恒为假，会把结果完全堵死）。
      return !state.getValue(PERSISTENT);
   }

   /** 六面中是否至少有一面暴露在空气中。 */
   private static boolean hasExposedFace(Level level, BlockPos pos) {
      for (Direction dir : Direction.values()) {
         if (level.getBlockState(pos.relative(dir)).isAir()) {
            return true;
         }
      }

      return false;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!state.getValue(HAS_PEACH)) {
         return super.useWithoutItem(state, level, pos, player, hit);
      }

      if (!level.isClientSide()) {
         level.setBlock(pos, state.setValue(HAS_PEACH, Boolean.FALSE), 3);
         ItemStack peach = new ItemStack(ModItems.PEACH.get());
         if (!player.addItem(peach)) {
            Block.popResource(level, pos, peach);
         }

         level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.2F);
      }

      return InteractionResult.SUCCESS;
   }

   /** 供树苗生成器判断：这是桃树叶方块。 */
   public static Block block() {
      return ModBlocks.PEACH_LEAVES.get();
   }
}
