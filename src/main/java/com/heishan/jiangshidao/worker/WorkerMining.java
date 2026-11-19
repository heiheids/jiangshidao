package com.heishan.jiangshidao.worker;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static com.heishan.jiangshidao.worker.WorkerHandler.WORK_RADIUS;
import static com.heishan.jiangshidao.worker.WorkerHandler.canOperateAt;
import static com.heishan.jiangshidao.worker.WorkerHandler.currentMoveTarget;
import static com.heishan.jiangshidao.worker.WorkerHandler.hint;
import static com.heishan.jiangshidao.worker.WorkerHandler.isBlocked;
import static com.heishan.jiangshidao.worker.WorkerHandler.walkTo;
import static com.heishan.jiangshidao.worker.WorkerStorage.deposit;
import static com.heishan.jiangshidao.worker.WorkerTools.takeAndUseTool;

/**
 * 工人任务：采石。
 *
 * <p>只挖石头与圆石（按需求方要求，不扩展到深板岩等其他方块）。
 *
 * <p>实现方式与耕种、砍树保持一致：
 * 选一个目标 → 用 {@code walkTo} 登记移动（每 tick 由调度层推进）→
 * 到位后取镐开采。不在这里做额外的"停滞/拉黑"判断，
 * 那套逻辑属于旧架构，会与每 tick 移动重复判定，导致目标被误拉黑而永远不开工。
 *
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
final class WorkerMining {
   private WorkerMining() {
   }

   static void mine(ServerLevel level, LivingEntity worker) {
      BlockPos origin = worker.blockPosition();
      BlockPos target = pickTarget(level, worker, origin);

      if (target == null) {
         hint(worker, "附近没有可挖的石头或圆石");
         return;
      }

      if (!canOperateAt(worker, target)) {
         // 走过去即可，移动由调度层每 tick 推进。
         walkTo(worker, target);
         return;
      }

      if (!takeAndUseTool(level, origin, worker, ItemTags.PICKAXES)) {
         hint(worker, "没有可用的镐子，暂停采石（放它手里、背包或附近箱子）");
         return;
      }

      BlockState state = level.getBlockState(target);
      List<ItemStack> drops = Block.getDrops(state, level, target, null);
      level.removeBlock(target, false);
      deposit(level, worker, origin, drops);
   }

   /**
    * 挑一块要挖的石头/圆石。
    *
    * <p>优先沿用已经在前往的目标，避免每轮因位置变化而改选，导致原地摆动。
    */
   private static BlockPos pickTarget(ServerLevel level, LivingEntity worker, BlockPos origin) {
      BlockPos existing = currentMoveTarget(worker);
      if (existing != null && isStone(level.getBlockState(existing)) && !isBlocked(existing)) {
         return existing;
      }

      BlockPos best = null;
      double bestDist = Double.MAX_VALUE;

      for (BlockPos pos : BlockPos.betweenClosed(
         origin.offset(-WORK_RADIUS, -8, -WORK_RADIUS),
         origin.offset(WORK_RADIUS, 8, WORK_RADIUS))) {

         BlockState state = level.getBlockState(pos);
         if (!isStone(state)) {
            continue;
         }

         if (level.getBlockEntity(pos) != null || isBlocked(pos)) {
            continue;
         }

         double dist = pos.distSqr(origin);
         if (dist < bestDist) {
            bestDist = dist;
            best = pos.immutable();
         }
      }

      return best;
   }

   /** 是否是本工种认可的矿块：石头或圆石。 */
   private static boolean isStone(BlockState state) {
      return state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE);
   }
}
