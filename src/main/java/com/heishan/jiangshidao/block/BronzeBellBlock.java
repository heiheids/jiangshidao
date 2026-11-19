package com.heishan.jiangshidao.block;

import com.heishan.jiangshidao.item.BronzeBellItem;
import com.heishan.jiangshidao.registry.ModDataComponents;
import com.heishan.jiangshidao.registry.ModEffects;
import com.heishan.jiangshidao.registry.ModItems;
import com.heishan.jiangshidao.worker.WorkerData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * BronzeBellBlock —— 方块定义与交互行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class BronzeBellBlock extends Block implements EntityBlock {
   private static final int BIND_RADIUS = 2;
   private static final int BIND_RADIUS_Y = 3;
   private static final double SEARCH_RADIUS = 32.0;
   private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 12.0, 12.0);

   public BronzeBellBlock(Properties properties) {
      super(properties);
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new BronzeBellBlockEntity(pos, state);
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.isClientSide() && !player.isCreative() && level instanceof ServerLevel server) {
         ItemStack drop = new ItemStack((ItemLike)ModItems.BRONZE_BELL.get());

         // 优先按方块实体记录的 UUID 精确找回绑定的怪物；
         // 只有方块实体没有记录时（例如旧存档），才退回按坐标就近扫描。
         LivingEntity bound = null;
         BronzeBellBlockEntity bell = BronzeBellBlockEntity.at(server, pos);
         if (bell != null && bell.monsterId() != null) {
            bound = findById(server, bell.monsterId());
         }

         if (bound == null) {
            bound = findBoundWorker(server, pos);
         }

         if (bound != null) {
            drop.set(ModDataComponents.BELL_CONTENT.get(), BronzeBellItem.pack(bound));
            WorkerData.setJob(drop, WorkerData.jobOf(bound));
            bound.discard();
         }

         popResource(level, pos, drop);
      }

      return super.playerWillDestroy(level, pos, state, player);
   }

   private static LivingEntity findById(ServerLevel level, java.util.UUID id) {
      for (ServerLevel dimension : level.getServer().getAllLevels()) {
         var entity = dimension.getEntity(id);
         if (entity instanceof LivingEntity living && living.isAlive()) {
            return living;
         }
      }

      return null;
   }

   private static LivingEntity findBoundWorker(ServerLevel level, BlockPos pos) {
      AABB box = new AABB(pos).inflate(32.0);
      LivingEntity best = null;
      double bestDist = Double.MAX_VALUE;

      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box)) {
         if (ModEffects.isSubdued(living)) {
            BlockPos home = WorkerData.homeOf(living);
            if (home != null && Math.abs(home.getX() - pos.getX()) <= 2 && Math.abs(home.getZ() - pos.getZ()) <= 2 && Math.abs(home.getY() - pos.getY()) <= 3) {
               double d = living.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
               if (d < bestDist) {
                  bestDist = d;
                  best = living;
               }
            }
         }
      }

      return best;
   }
}
