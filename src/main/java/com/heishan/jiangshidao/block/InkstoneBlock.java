package com.heishan.jiangshidao.block;

import com.heishan.jiangshidao.registry.ModDataComponents;
import com.heishan.jiangshidao.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * InkstoneBlock —— 方块定义与交互行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class InkstoneBlock extends Block implements EntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final VoxelShape SHAPE_NS = Shapes.box(0.25, 0.0, 0.3125, 0.75, 0.1875, 0.6875);
   private static final VoxelShape SHAPE_EW = Shapes.box(0.3125, 0.0, 0.25, 0.6875, 0.1875, 0.75);

   public InkstoneBlock() {
      super(BlockProps.of("inkstone").mapColor(MapColor.COLOR_BLACK).strength(1.5F, 6.0F).sound(SoundType.STONE).noOcclusion().requiresCorrectToolForDrops());
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return (BlockState)this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
   }

   private static VoxelShape shapeFor(BlockState state) {
      Direction d = (Direction)state.getValue(FACING);
      return d != Direction.EAST && d != Direction.WEST ? SHAPE_NS : SHAPE_EW;
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return shapeFor(state);
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return shapeFor(state);
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new InkstoneBlockEntity(pos, state);
   }

   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!(level.getBlockEntity(pos) instanceof InkstoneBlockEntity ink)) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      } else if (!stack.is(ModItems.CINNABAR_POWDER.get()) && !stack.is(Items.CINNABAR)) {
         if (isWaterContainer(stack)) {
            if (level.isClientSide()) {
               return InteractionResult.SUCCESS;
            }

            if (ink.hasWater()) {
               player.sendOverlayMessage(Component.literal("§e砚台里已经有水了"));
            }

            ink.addWater();
            if (!player.getAbilities().instabuild) {
               if (stack.is(Items.POTION)) {
                  stack.shrink(1);
                  if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
                     Block.popResource(player.level(), player.blockPosition(), new ItemStack(Items.GLASS_BOTTLE));
                  }
               } else if (stack.is(Items.WATER_BUCKET)) {
                  stack.shrink(1);
                  if (!player.getInventory().add(new ItemStack(Items.BUCKET))) {
                     Block.popResource(player.level(), player.blockPosition(), new ItemStack(Items.BUCKET));
                  }
               }
            }

            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 1.0F);
            player.sendOverlayMessage(Component.literal("§b已加水"));
            return InteractionResult.SUCCESS;
         } else {
            if (stack.is(ModItems.BRUSH.get())) {
               if (level.isClientSide()) {
                  return InteractionResult.SUCCESS;
               }

               if (!ink.hasWater()) {
                  player.sendOverlayMessage(Component.literal("§7砚台里没有水，无法研墨"));
               }

               if (ink.cinnabarCount() <= 0) {
                  player.sendOverlayMessage(Component.literal("§7砚台里没有朱砂了"));
                  return InteractionResult.SUCCESS;
               }

               ink.consumeOneCinnabar();
               stack.set(ModDataComponents.INKED.get(), Boolean.TRUE);
               level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.1F);
               player.sendOverlayMessage(Component.literal("§c毛笔已沾上朱砂墨"));
            }

            if (stack.isEmpty()) {
               if (!level.isClientSide()) {
                  player.sendOverlayMessage(Component.literal(ink.statusText()));
               }

               return InteractionResult.SUCCESS;
            } else {
               return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
         }
      } else {
         if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
         }

         if (ink.isCinnabarFull()) {
            player.sendOverlayMessage(Component.literal("§c砚台里的朱砂已满（64）"));
            level.playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.4F, 0.8F);
            return InteractionResult.SUCCESS;
         }

         int space = ink.remainingCinnabarCapacity();
         int toAdd = Math.min(8, space);
         if (!player.getAbilities().instabuild) {
            toAdd = Math.min(toAdd, stack.getCount());
         }

         for (int n = 0; n < toAdd; n++) {
            ink.addCinnabar();
         }

         if (!player.getAbilities().instabuild) {
            stack.shrink(toAdd);
         }

         level.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.7F, 1.4F);
         player.sendOverlayMessage(Component.literal("§a朱砂 +" + toAdd + "  §7(" + ink.cinnabarCount() + "/64)"));
         return InteractionResult.SUCCESS;
      }
   }

   private static boolean isWaterContainer(ItemStack stack) {
      if (stack.is(Items.WATER_BUCKET)) {
         return true;
      }

      if (!stack.is(Items.POTION)) {
         return false;
      }

      PotionContents contents = (PotionContents)stack.get(DataComponents.POTION_CONTENTS);
      return contents != null && contents.is(Potions.WATER);
   }

   protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
      super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
   }
}
