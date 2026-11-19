package com.heishan.jiangshidao.block;

import com.heishan.jiangshidao.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * StoneMillBlock —— 方块定义与交互行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class StoneMillBlock extends Block implements EntityBlock {
   public static final BooleanProperty UPPER = BooleanProperty.create("upper");

   public StoneMillBlock() {
      super(BlockProps.of("stone_mill").mapColor(MapColor.STONE).strength(2.0F, 6.0F).sound(SoundType.STONE).noOcclusion().requiresCorrectToolForDrops());
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(UPPER, Boolean.FALSE));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{UPPER});
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new StoneMillBlockEntity(pos, state);
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return type != ModBlockEntities.STONE_MILL.get() ? null : (lvl, pos, st, be) -> {
         if (be instanceof StoneMillBlockEntity mill) {
            mill.tick();
         }
      };
   }

   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (level.getBlockEntity(pos) instanceof StoneMillBlockEntity mill) {
         if (StoneMillBlockEntity.isValidInput(stack)) {
            if (level.isClientSide()) {
               return InteractionResult.SUCCESS;
            }

            boolean ok = mill.tryInsert(stack);
            if (ok) {
               if (!player.getAbilities().instabuild) {
                  stack.shrink(1);
               }

               level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.7F, 1.2F);
            } else {
               level.playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.4F, 0.8F);
            }

            return InteractionResult.SUCCESS;
         } else {
            mill.onHoldPulse();
            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }
   }
}
