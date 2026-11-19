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
import static com.heishan.jiangshidao.worker.WorkerHandler.*;
import static com.heishan.jiangshidao.worker.WorkerFarming.*;
import static com.heishan.jiangshidao.worker.WorkerFishing.*;
import static com.heishan.jiangshidao.worker.WorkerWoodcutting.*;
import static com.heishan.jiangshidao.worker.WorkerMining.*;
import static com.heishan.jiangshidao.worker.WorkerTools.*;
import static com.heishan.jiangshidao.worker.WorkerStorage.*;

/**
 * 工人任务：背包与容器之间的存取投递。
 * 由 {@link WorkerHandler} 调度，方法体自 WorkerHandler 原样拆出。
 */
final class WorkerStorage {
   private WorkerStorage() {
   }

   static void deposit(ServerLevel level, LivingEntity worker, BlockPos origin, List<ItemStack> stacks) {
      WorkerBackpack backpack = backpackOf(worker, level.registryAccess());

      for (ItemStack stack : stacks) {
         if (!stack.isEmpty()) {
            mergeIntoBackpack(backpack, stack);
            if (!stack.isEmpty()) {
               flushBackpack(level, worker, origin);
               mergeIntoBackpack(backpack, stack);
               if (!stack.isEmpty() && !insertIntoContainers(level, worker, origin, stack)) {
                  Block.popResource(level, worker.blockPosition(), stack.copy());
                  stack.setCount(0);
                  hint(worker, "背包和附近箱子都满了，东西掉在脚边");
               }
            }
         }
      }

      if (usedSlots(backpack) >= 20) {
         flushBackpack(level, worker, origin);
      }
   }

   static void flushBackpack(ServerLevel level, LivingEntity worker, BlockPos origin) {
      WorkerBackpack backpack = backpackOf(worker, level.registryAccess());

      for (int i = 0; i < backpack.getContainerSize(); i++) {
         ItemStack stored = backpack.getItem(i);
         if (!stored.isEmpty() && insertIntoContainers(level, worker, origin, stored)) {
            backpack.setItem(i, ItemStack.EMPTY);
         }
      }
   }

   static void mergeIntoBackpack(WorkerBackpack backpack, ItemStack stack) {
      for (int i = 0; i < backpack.getContainerSize() && !stack.isEmpty(); i++) {
         ItemStack slot = backpack.getItem(i);
         if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
            int move = Math.min(slot.getMaxStackSize() - slot.getCount(), stack.getCount());
            slot.grow(move);
            stack.shrink(move);
            backpack.setChanged();
         }
      }

      for (int i = 0; i < backpack.getContainerSize() && !stack.isEmpty(); i++) {
         if (backpack.getItem(i).isEmpty()) {
            int move = Math.min(stack.getMaxStackSize(), stack.getCount());
            ItemStack placed = stack.copy();
            placed.setCount(move);
            backpack.setItem(i, placed);
            stack.shrink(move);
            backpack.setChanged();
         }
      }
   }

   static int usedSlots(WorkerBackpack backpack) {
      int used = 0;

      for (int i = 0; i < backpack.getContainerSize(); i++) {
         if (!backpack.getItem(i).isEmpty()) {
            used++;
         }
      }

      return used;
   }

   static boolean insertIntoContainers(ServerLevel level, LivingEntity worker, BlockPos origin, ItemStack stack) {
      // 以铜铃为中心扫描：工人常在 20 格内的外围干活，箱子放在铃旁，
      // 用自身位置当原点会扫不到（这也是此前"取不到/放不回箱子"的原因）。
      BlockPos scanOrigin = centerOf(worker);

      for (BlockPos pos : BlockPos.betweenClosed(scanOrigin.offset(-WORK_RADIUS, -8, -WORK_RADIUS), scanOrigin.offset(WORK_RADIUS, 8, WORK_RADIUS))) {
         if (level.getBlockEntity(pos) instanceof Container container && tryPut(container, stack)) {
            container.setChanged();
            return true;
         }
      }

      return false;
   }

   static boolean tryPut(Container container, ItemStack stack) {
      for (int i = 0; i < container.getContainerSize(); i++) {
         ItemStack slot = container.getItem(i);
         if (slot.isEmpty()) {
            container.setItem(i, stack.copy());
            stack.setCount(0);
            return true;
         }

         if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
            int space = slot.getMaxStackSize();
            int move = Math.min(space - slot.getCount(), stack.getCount());
            slot.grow(move);
            stack.shrink(move);
            if (stack.isEmpty()) {
               return true;
            }
         }
      }

      return stack.isEmpty();
   }
}
