package com.heishan.jiangshidao.block;

import com.heishan.jiangshidao.item.BrushItem;
import com.heishan.jiangshidao.registry.ModDataComponents;
import com.heishan.jiangshidao.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
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
 * TalismanPaperBlock —— 方块定义与交互行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class TalismanPaperBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final VoxelShape SHAPE_NS = Block.box(4.0, 0.0, 0.0, 12.0, 1.0, 16.0);
   private static final VoxelShape SHAPE_EW = Block.box(0.0, 0.0, 4.0, 16.0, 1.0, 12.0);

   public TalismanPaperBlock(Properties properties) {
      super(properties);
   }

   public static Properties defaultProperties() {
      return BlockProps.of("talisman_paper").mapColor(MapColor.COLOR_YELLOW).noCollision().instabreak().noOcclusion().sound(SoundType.WOOL);
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return shapeFor(state);
   }

   private static VoxelShape shapeFor(BlockState state) {
      Direction facing = (Direction)state.getValue(FACING);
      return facing != Direction.NORTH && facing != Direction.SOUTH ? SHAPE_EW : SHAPE_NS;
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
      return shapeFor(state);
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      BlockPos below = pos.below();
      BlockState belowState = level.getBlockState(below);
      return belowState.isFaceSturdy(level, below, Direction.UP);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
      return this.canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (stack.is(ModItems.BRUSH.get())) {
         if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
         }

         writeTalisman(level, pos, player, stack);
         return InteractionResult.SUCCESS;
      } else if (stack.isEmpty()) {
         if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TalismanPaperBlockEntity be) {
            player.sendOverlayMessage(be.statusText());
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }
   }

   public static boolean writeTalisman(Level level, BlockPos pos, Player player, ItemStack brush) {
      if (level.getBlockEntity(pos) instanceof TalismanPaperBlockEntity be) {
         if (!be.hasSelection()) {
            player.sendOverlayMessage(Component.translatable("message.jiangshidao.paper.noselect"));
            return false;
         }

         if (!BrushItem.isInked(brush)) {
            player.sendOverlayMessage(Component.translatable("message.jiangshidao.paper.noink"));
            return false;
         }

         ItemStack produced = createTalisman(be.selectedType());
         if (produced.isEmpty()) {
            return false;
         }

         brush.set(ModDataComponents.INKED.get(), Boolean.FALSE);
         if (!player.getInventory().add(produced)) {
            Block.popResource(player.level(), player.blockPosition(), produced);
         }

         level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.2F);
         if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("message.jiangshidao.paper.written", new Object[]{produced.getHoverName()}));
         }

         boolean refilled = consumeBlankPaper(player);
         if (refilled) {
            if (player instanceof ServerPlayer sp2) {
               sp2.sendOverlayMessage(Component.translatable("message.jiangshidao.paper.refilled"));
            }
         } else {
            level.removeBlock(pos, false);
         }

         return true;
      } else {
         return false;
      }
   }

   public static ItemStack createTalisman(String type) {
      return switch (type) {
         case "sealing" -> new ItemStack((ItemLike)ModItems.SEALING_TALISMAN.get());
         case "burning" -> new ItemStack((ItemLike)ModItems.BURNING_TALISMAN.get());
         case "thunder" -> new ItemStack((ItemLike)ModItems.THUNDER_TALISMAN.get());
         case "subjugation" -> new ItemStack((ItemLike)ModItems.SUBJUGATION_TALISMAN.get());
         default -> ItemStack.EMPTY;
      };
   }

   public static boolean consumeBlankPaper(Player player) {
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack s = player.getInventory().getItem(i);
         if (s.is(ModItems.BLANK_TALISMAN_PAPER.get())) {
            s.shrink(1);
            if (s.isEmpty()) {
               player.getInventory().setItem(i, ItemStack.EMPTY);
            }

            return true;
         }
      }

      return false;
   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.isClientSide() && !player.isCreative()) {
         ItemStack paper = new ItemStack((ItemLike)ModItems.BLANK_TALISMAN_PAPER.get());
         if (!player.getInventory().add(paper)) {
            Block.popResource(level, pos, paper);
         }
      }

      return super.playerWillDestroy(level, pos, state, player);
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new TalismanPaperBlockEntity(pos, state);
   }
}
