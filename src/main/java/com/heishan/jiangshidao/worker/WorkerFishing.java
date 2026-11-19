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
 * 工人任务：垂钓、附魔加成与战利品表。
 * 由 {@link WorkerHandler} 调度，方法体自 WorkerHandler 原样拆出。
 */
final class WorkerFishing {
   private WorkerFishing() {
   }

   static void fish(ServerLevel level, LivingEntity worker) {
      BlockPos origin = worker.blockPosition();
      if (!hasWaterNearby(level, worker)) {
         hint(worker, "附近没有可以钓鱼的水");
      } else {
         equipRodFromStorage(level, worker, origin);
         ItemStack rod = findFishingRod(level, worker, origin);
         if (rod.isEmpty()) {
            hint(worker, "需要一根鱼竿（放它手里或附近箱子里）");
         } else {
            int lure = enchantLevel(level, rod, Enchantments.LURE);
            int luck = enchantLevel(level, rod, Enchantments.LUCK_OF_THE_SEA);
            double bonus = fishingBonus(worker);
            double treasureChance = Math.min(0.95, (0.1 + luck * 0.02) * bonus);
            List<ItemStack> catch_ = new ArrayList<>(rollVanillaFishing(level, worker, rod, luck));
            double extraChance = Math.min(0.9, lure * 0.1 * bonus);
            if (level.getRandom().nextDouble() < extraChance) {
               catch_.addAll(rollVanillaFishing(level, worker, rod, luck));
            }

            deposit(level, worker, origin, catch_);
            if (enchantLevel(level, rod, Enchantments.MENDING) <= 0) {
               EquipmentSlot rodSlot = worker.getMainHandItem() == rod ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
               rod.hurtAndBreak(1, worker, rodSlot);
               if (rod.isEmpty()) {
                  hint(worker, "鱼竿用坏了，记得往箱子里补竿");
               }
            }
         }
      }
   }

   static double fishingBonus(LivingEntity worker) {
      int level = TalentSet.of(worker).level(Talent.FISHING);

      return switch (level) {
         case 1 -> 1.15;
         case 2 -> 1.36;
         case 3 -> 1.57;
         case 4 -> 1.79;
         case 5 -> 2.0;
         default -> 1.0;
      };
   }

   static int enchantLevel(ServerLevel level, ItemStack stack, ResourceKey<Enchantment> key) {
      try {
         return EnchantmentHelper.getItemEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), stack);
      } catch (RuntimeException ignored) {
         return 0;
      }
   }

   static void equipRodFromStorage(ServerLevel level, LivingEntity worker, BlockPos origin) {
      if (!worker.getMainHandItem().is(Items.FISHING_ROD)) {
         WorkerBackpack backpack = backpackOf(worker, level.registryAccess());

         for (int i = 0; i < backpack.getContainerSize(); i++) {
            ItemStack slot = backpack.getItem(i);
            if (slot.is(Items.FISHING_ROD)) {
               ItemStack rod = slot.copyWithCount(1);
               slot.shrink(1);
               backpack.setChanged();
               swapIntoMainHand(level, worker, backpack, rod);
               hint(worker, "从背包里拿出了鱼竿");
               return;
            }
         }

         for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-WORK_RADIUS, -2, -WORK_RADIUS), origin.offset(WORK_RADIUS, 2, WORK_RADIUS))) {
            if (level.getBlockEntity(pos) instanceof Container container) {
               for (int i = 0; i < container.getContainerSize(); i++) {
                  ItemStack slot = container.getItem(i);
                  if (slot.is(Items.FISHING_ROD)) {
                     ItemStack rod = slot.copyWithCount(1);
                     slot.shrink(1);
                     container.setChanged();
                     swapIntoMainHand(level, worker, backpack, rod);
                     hint(worker, "从箱子里换上了鱼竿");
                     return;
                  }
               }
            }
         }
      }
   }

   static void swapIntoMainHand(ServerLevel level, LivingEntity worker, WorkerBackpack backpack, ItemStack rod) {
      ItemStack old = worker.getMainHandItem();
      if (!old.isEmpty()) {
         ItemStack leftover = old.copy();
         worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
         mergeIntoBackpack(backpack, leftover);
         if (!leftover.isEmpty()) {
            Block.popResource(level, worker.blockPosition(), leftover);
         }
      }

      worker.setItemSlot(EquipmentSlot.MAINHAND, rod);
   }

   static ItemStack findFishingRod(ServerLevel level, LivingEntity worker, BlockPos origin) {
      for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
         ItemStack held = worker.getItemBySlot(slot);
         if (held.is(Items.FISHING_ROD)) {
            return held;
         }
      }

      WorkerBackpack backpack = backpackOf(worker, level.registryAccess());

      for (int i = 0; i < backpack.getContainerSize(); i++) {
         if (backpack.getItem(i).is(Items.FISHING_ROD)) {
            return backpack.getItem(i);
         }
      }

      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-WORK_RADIUS, -2, -WORK_RADIUS), origin.offset(WORK_RADIUS, 2, WORK_RADIUS))) {
         if (level.getBlockEntity(pos) instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
               if (container.getItem(i).is(Items.FISHING_ROD)) {
                  return container.getItem(i);
               }
            }
         }
      }

      return ItemStack.EMPTY;
   }

   static List<ItemStack> rollVanillaFishing(ServerLevel level, LivingEntity worker, ItemStack rod, int luck) {
      try {
         LootParams params = new Builder(level)
            .withParameter(LootContextParams.ORIGIN, worker.position())
            .withParameter(LootContextParams.TOOL, rod)
            .withParameter(LootContextParams.THIS_ENTITY, worker)
            .withLuck(luck)
            .create(LootContextParamSets.FISHING);
         LootTable table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
         return table.getRandomItems(params);
      } catch (RuntimeException e) {
         List<ItemStack> fallback = new ArrayList<>();
         fallback.add(new ItemStack(Items.COD));
         return fallback;
      }
   }

   static Item randomTreasure(ServerLevel level) {
      Item[] pool = new Item[]{Items.NAME_TAG, Items.SADDLE, Items.NAUTILUS_SHELL, Items.BOW, Items.FISHING_ROD, Items.ENCHANTED_BOOK, Items.LILY_PAD};
      return pool[level.getRandom().nextInt(pool.length)];
   }

   static boolean hasWaterNearby(ServerLevel level, LivingEntity worker) {
      BlockPos origin = worker.blockPosition();
      BlockPos memo = WATER_MEMO.get(worker);
      if (memo != null) {
         if (level.getBlockState(memo).is(Blocks.WATER)) {
            return true;
         }

         WATER_MEMO.remove(worker);
      }

      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-WORK_RADIUS, -3, -WORK_RADIUS), origin.offset(WORK_RADIUS, 3, WORK_RADIUS))) {
         if (level.getBlockState(pos).is(Blocks.WATER)) {
            WATER_MEMO.put(worker, pos.immutable());
            return true;
         }
      }

      return false;
   }

   static boolean containsFishingRod(Container container) {
      for (int i = 0; i < container.getContainerSize(); i++) {
         if (container.getItem(i).is(Items.FISHING_ROD)) {
            return true;
         }
      }

      return false;
   }

   static Item randomFish(ServerLevel level) {
      return switch (level.getRandom().nextInt(10)) {
         case 0 -> Items.TROPICAL_FISH;
         case 1, 2 -> Items.PUFFERFISH;
         case 3, 4, 5 -> Items.SALMON;
         default -> Items.COD;
      };
   }
}