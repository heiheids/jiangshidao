package com.heishan.jiangshidao.worker;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.entity.GhostWomanEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.Set;
import java.util.HashSet;
import java.util.Deque;
import java.util.ArrayDeque;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

// 拆分后的跨类静态导入：方法体保持原样，裸调用在此解析。
import static com.heishan.jiangshidao.worker.WorkerHandler.isBlocked;
import static com.heishan.jiangshidao.worker.WorkerHandler.*;
import static com.heishan.jiangshidao.worker.WorkerFarming.*;
import static com.heishan.jiangshidao.worker.WorkerFishing.*;
import static com.heishan.jiangshidao.worker.WorkerWoodcutting.*;
import static com.heishan.jiangshidao.worker.WorkerMining.*;
import static com.heishan.jiangshidao.worker.WorkerTools.*;
import static com.heishan.jiangshidao.worker.WorkerStorage.*;

/**
 * 工人任务：伐木、树叶清理与树苗补种。
 * 由 {@link WorkerHandler} 调度，方法体自 WorkerHandler 原样拆出。
 */
final class WorkerWoodcutting {
   /** 单棵树允许的最大砍伐高度，超过则需飞行工人处理。 */
   private static final int MAX_CHOP_HEIGHT = 8;
   private WorkerWoodcutting() {
   }

   static void chopWood(ServerLevel level, LivingEntity worker) {
      // 以铜铃为中心搜索，扩大工作范围；"最近"仍按工人自身位置度量。
      BlockPos origin = worker.blockPosition();
      BlockPos target = findChoppingTarget(level, worker, origin);
      if (target == null) {
         hint(worker, "附近没有可以砍的树");
      } else if (!canOperateAt(worker, target)) {
         if (!clearBlockingLeaves(level, worker, origin)) {
            walkTo(worker, target);
         }
      } else if (!takeAndUseTool(level, origin, worker, ItemTags.AXES)) {
         hint(worker, "需要一把斧子（放它手里或背包里）");
      } else {
         List<ItemStack> drops = new ArrayList<>();
         // 用洪水填充砍整棵树：树干可能分叉、玩家放置的原木也会连在一起，
         // 只按单列 Y 轴循环会漏掉分叉部分（表现为"有原木没砍掉"）。
         Set<BlockPos> visited = new HashSet<>();
         Deque<BlockPos> queue = new ArrayDeque<>();
         queue.add(target.immutable());

         while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!visited.add(pos)) {
               continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LOGS) || level.getBlockEntity(pos) != null) {
               continue;
            }

            drops.addAll(Block.getDrops(state, level, pos, null));
            level.removeBlock(pos, false);

            // 只向六邻接扩散，且限制在合理高度内，避免顺着建筑滥用。
            for (Direction dir : Direction.values()) {
               BlockPos next = pos.relative(dir);
               if (visited.contains(next)) {
                  continue;
               }

               if (next.getY() < target.getY() - 1 || next.getY() > target.getY() + MAX_CHOP_HEIGHT) {
                  continue;
               }

               if (level.getBlockState(next).is(BlockTags.LOGS)) {
                  queue.add(next);
               }
            }
         }

         drops.addAll(fellLeaves(level, target, trunkBaseY(level, target), trunkTopY(level, target)));
         deposit(level, worker, origin, drops);
         plantSaplingsFromBackpack(level, worker, origin);
         boostSaplings(level, worker, origin);
      }
   }

   static BlockPos findChoppingTarget(ServerLevel level, LivingEntity worker, BlockPos origin) {
      BlockPos best = null;
      double bestDist = Double.MAX_VALUE;
      boolean tooTallSkipped = false;

      // 搜索范围覆盖整个工作半径（以铜铃为中心），垂直方向给足树高余量。
      for (BlockPos pos : BlockPos.betweenClosed(
         origin.offset(-WORK_RADIUS, -3, -WORK_RADIUS),
         origin.offset(WORK_RADIUS, MAX_CHOP_HEIGHT + 4, WORK_RADIUS))) {

         BlockState state = level.getBlockState(pos);
         if (!state.is(BlockTags.LOGS) || level.getBlockEntity(pos) != null) {
            continue;
         }

         if (isBlocked(pos)) {
            continue;
         }

         // 只认"树根"：下方不是原木，说明这是这棵树的底部。
         // 否则同一棵树会被重复选中，且玩家随手放的原木也会被当成树砍掉。
         if (level.getBlockState(pos.below()).is(BlockTags.LOGS)) {
            continue;
         }

         // 自然生长的树底部一定站在可种植方块（泥土/草方块等）上；
         // 玩家放在地面上的一摞原木同样满足"下方不是原木"，用这个条件排除。
         BlockState ground = level.getBlockState(pos.below());
         if (!ground.is(BlockTags.DIRT) && !ground.is(Blocks.GRASS_BLOCK) && !ground.is(Blocks.FARMLAND)) {
            continue;
         }

         int baseY = trunkBaseY(level, pos);
         int topY = trunkTopY(level, pos);
         int height = topY - baseY + 1;
         if (height > MAX_CHOP_HEIGHT && !isFlyingWorker(worker)) {
            tooTallSkipped = true;
            continue;
         }

         double dist = worker.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
         if (dist < bestDist) {
            bestDist = dist;
            best = pos.immutable();
         }
      }

      if (best == null && tooTallSkipped) {
         hint(worker, "只找到超过 " + MAX_CHOP_HEIGHT + " 格的巨树，需要会飞的工人（女鬼）");
      }

      return best;
   }

   static boolean isFlyingWorker(LivingEntity worker) {
      return worker instanceof GhostWomanEntity;
   }

   static int trunkBaseY(ServerLevel level, BlockPos pos) {
      int y = pos.getY();

      while (y > level.getMinY() && level.getBlockState(new BlockPos(pos.getX(), y - 1, pos.getZ())).is(BlockTags.LOGS)) {
         y--;
      }

      return y;
   }

   static int trunkTopY(ServerLevel level, BlockPos pos) {
      int y = pos.getY();

      while (y < level.getMaxY() - 1 && level.getBlockState(new BlockPos(pos.getX(), y + 1, pos.getZ())).is(BlockTags.LOGS)) {
         y++;
      }

      return y;
   }

   static List<ItemStack> fellLeaves(ServerLevel level, BlockPos trunk, int baseY, int topY) {
      List<ItemStack> drops = new ArrayList<>();

      for (BlockPos pos : BlockPos.betweenClosed(
         new BlockPos(trunk.getX() - 7, baseY - 2, trunk.getZ() - 7), new BlockPos(trunk.getX() + 7, topY + 4, trunk.getZ() + 7)
      )) {
         BlockState state = level.getBlockState(pos);
         if (state.is(BlockTags.LEAVES)) {
            double dx = pos.getX() - trunk.getX();
            double dz = pos.getZ() - trunk.getZ();
            if (!(dx * dx + dz * dz > 49.0)) {
               drops.addAll(Block.getDrops(state, level, pos, null));
               level.removeBlock(pos.immutable(), false);
            }
         }
      }

      return drops;
   }

   static boolean clearBlockingLeaves(ServerLevel level, LivingEntity worker, BlockPos origin) {
      boolean cleared = false;
      List<ItemStack> drops = new ArrayList<>();

      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -1, -3), origin.offset(3, 3, 3))) {
         BlockState state = level.getBlockState(pos);
         if (state.is(BlockTags.LEAVES) && canOperateAt(worker, pos)) {
            drops.addAll(Block.getDrops(state, level, pos, null));
            level.removeBlock(pos.immutable(), false);
            cleared = true;
         }
      }

      if (cleared) {
         deposit(level, worker, origin, drops);
      }

      return cleared;
   }
}