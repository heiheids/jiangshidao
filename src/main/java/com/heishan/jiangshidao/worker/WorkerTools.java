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
 * 工人任务：工具取用与耐久消耗。
 * 由 {@link WorkerHandler} 调度，方法体自 WorkerHandler 原样拆出。
 */
final class WorkerTools {
   private WorkerTools() {
   }

   /**
    * 工具收纳间隔（tick）。收纳涉及扫描周围箱子，不需要每轮都做，
    * 10 秒检查一次即可，避免频繁遍历方块实体。
    */
   static final int STOW_INTERVAL = 200;

   /**
    * 贵重工具（钻石及以上材质）保留的耐久余量。
    *
    * <p>剩余耐久低于此值就把工具收进箱子/背包，换下一把继续干活，
    * 避免把玩家辛苦做的高级工具用坏。
    */
   static final int PRECIOUS_RESERVE = 20;

   /** 每只工人上次尝试把多余工具放回箱子的时间。 */
   private static final Map<Integer, Long> LAST_STOW = new ConcurrentHashMap<>();

   /**
    * 保证工人身上只保留一件工具。
    *
    * <p>规则：手中（主手/副手）只允许存在当前作业需要的那一件工具。
    * 不匹配的工具按以下顺序处理：
    * <ol>
    *   <li>放进铜铃周围的箱子（有空位时）；</li>
    *   <li>箱子没空位就留在工人背包里；</li>
    *   <li>之后每隔 {@link #STOW_INTERVAL} 再尝试一次入箱，成功为止。</li>
    * </ol>
    *
    * @param tag 当前作业需要的工具类别
    */
   static void keepOnlyOneTool(ServerLevel level, LivingEntity worker, TagKey<Item> tag) {
      ItemStack main = worker.getMainHandItem();
      ItemStack off = worker.getOffhandItem();

      // 副手一律清空：身上只允许一件工具。
      if (!off.isEmpty()) {
         stowOrBackpack(level, worker, off);
         worker.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
      }

      // 主手工具不匹配当前作业时，先收起来再取新工具。
      if (!main.isEmpty() && !main.is(tag)) {
         stowOrBackpack(level, worker, main);
         worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      }
   }

   /**
    * 把一件物品收起来：优先入箱，箱子没空位就进背包。
    */
   private static void stowOrBackpack(ServerLevel level, LivingEntity worker, ItemStack stack) {
      if (stack.isEmpty()) {
         return;
      }

      int id = worker.getId();
      long now = level.getGameTime();
      Long last = LAST_STOW.get(id);

      // 入箱是"有空位就做"的操作，但扫描箱子较贵，按间隔节流。
      if (last == null || now - last >= STOW_INTERVAL) {
         LAST_STOW.put(id, now);
         ItemStack copy = stack.copy();
         if (insertIntoContainers(level, worker, worker.blockPosition(), copy) && copy.isEmpty()) {
            return;   // 已经全部进箱
         }
      }

      // 箱子放不下（或还没到检查时间）：留在背包里，之后会再次尝试入箱。
      mergeIntoBackpack(backpackOf(worker, level.registryAccess()), stack);
   }

   public static boolean takeAndUseTool(ServerLevel level, BlockPos origin, LivingEntity worker, TagKey<Item> tag) {
      // 先把身上不属于当前作业的工具收起来，保证全程只带一件。
      keepOnlyOneTool(level, worker, tag);

      for (int round = 0; round < 5; round++) {
         ItemStack tool = takeAnyTool(level, origin, worker, tag);
         if (tool == null) {
            // 没有可用工具时，把手上残留的坏工具清掉，避免"举着个坏镐"的观感，
            // 同时让调用方能明确知道这次没拿到工具。
            if (worker.getMainHandItem().is(tag)) {
               worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }

            return round > 0;
         }

         // 钻石及以上：耐久快见底就收起来保存，不把它用坏。
         if (isPrecious(tool) && remainingDurability(tool) <= PRECIOUS_RESERVE) {
            stowPreciousTool(level, worker, tool);
            hint(worker, "贵重的工具快用坏了，已收好，正在换一把");
            continue;
         }

         boolean broken = useUp(tool);
         if (broken) {
            if (isPrecious(tool)) {
               // 理论上不会走到这里（上面已提前收纳），保险起见也不让它碎掉
               hint(worker, "贵重工具已保护，未被用坏");
               stowPreciousTool(level, worker, tool);
               continue;
            }

            // 低级别工具：就地报废，主手清空后继续找下一把。
            // 之前这里会把"已报废"的同一把工具写回主手并直接返回，
            // 导致 1 点耐久的工具每轮都判定为坏、却始终留在手上，永远用不完。
            worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            hint(worker, "工具用坏了，正在找备用工具");
            continue;
         }

         // 成功拿到并消耗一次耐久后，必须真正放回主手 —— 渲染器只渲染主手/副手，
         // 之前取工具时会先清空主手，若工具判废则主手彻底空掉，看起来就是"手上没有镐"。
         worker.setItemSlot(EquipmentSlot.MAINHAND, tool);
         return true;
      }

      return true;
   }

   /** 钻石及以上材质视为贵重工具，不允许用坏。 */
   static boolean isPrecious(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
      return path.startsWith("diamond_") || path.startsWith("netherite_");
   }

   /** 剩余耐久。 */
   static int remainingDurability(ItemStack stack) {
      if (stack.getMaxDamage() <= 0) {
         return Integer.MAX_VALUE;
      }

      return stack.getMaxDamage() - stack.getDamageValue();
   }

   /**
    * 把贵重工具妥善收起来：优先放进附近箱子，箱子没位置就留在工人背包。
    */
   private static void stowPreciousTool(ServerLevel level, LivingEntity worker, ItemStack tool) {
      worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

      ItemStack copy = tool.copy();
      if (insertIntoContainers(level, worker, worker.blockPosition(), copy) && copy.isEmpty()) {
         return;
      }

      mergeIntoBackpack(backpackOf(worker, level.registryAccess()), tool);
   }

   static ItemStack takeAnyTool(ServerLevel level, BlockPos origin, LivingEntity worker, TagKey<Item> tag) {
      for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
         ItemStack held = worker.getItemBySlot(slot);
         if (!held.isEmpty() && held.is(tag) && held.getMaxDamage() > 0) {
            // 用 copy 返回给调用方去消耗耐久；原槽位保持不变，
            // 由调用方在确认可用后再写回主手。否则取出的工具一旦判废，
            // 手上就永远空空如也（渲染上表现为"手上没有镐"）。
            return held.copy();
         }
      }

      WorkerBackpack backpack = backpackOf(worker, worker.level().registryAccess());

      for (int i = 0; i < backpack.getContainerSize(); i++) {
         ItemStack tool = backpack.getItem(i);
         if (!tool.isEmpty() && tool.is(tag) && tool.getMaxDamage() > 0) {
            ItemStack taken = tool.copy();
            backpack.setItem(i, ItemStack.EMPTY);
            backpack.setChanged();
            return taken;
         }
      }

      // 箱子里的工具以铜铃为中心搜索，而不是以工人为中心。
      // 原因：箱子通常放在铜铃旁边（工作区仓储点），而工人会在 20 格内的外围干活，
      // 若以工人为圆心，跑到远处时就搜不到铃边的箱子，表现为"从不自动换工具"。
      BlockPos storageCenter = centerOf(worker);
      for (BlockPos pos : BlockPos.betweenClosed(
         storageCenter.offset(-WORK_RADIUS, -8, -WORK_RADIUS),
         storageCenter.offset(WORK_RADIUS, 8, WORK_RADIUS))) {
         if (level.getBlockEntity(pos) instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
               ItemStack tool = container.getItem(i);
               if (!tool.isEmpty() && tool.is(tag) && tool.getMaxDamage() > 0) {
                  ItemStack taken = tool.copy();
                  container.setItem(i, ItemStack.EMPTY);
                  container.setChanged();
                  return taken;
               }
            }
         }
      }

      return null;
   }

   static boolean useUp(ItemStack tool) {
      if (tool.getMaxDamage() <= 0) {
         return false;
      }

      int damage = tool.getDamageValue() + 1;
      if (damage >= tool.getMaxDamage()) {
         return true;
      }

      tool.setDamageValue(damage);
      return false;
   }
}
