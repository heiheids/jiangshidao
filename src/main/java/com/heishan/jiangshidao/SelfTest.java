package com.heishan.jiangshidao;

import com.heishan.jiangshidao.accessory.AccessorySlot;
import com.heishan.jiangshidao.item.ChunyangJinItem;
import com.heishan.jiangshidao.registry.ModDataComponents;
import com.heishan.jiangshidao.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * SelfTest。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class SelfTest {
   private static final String NS = JiangshiDao.MOD_ID;

   public static void register() {
      if (System.getenv("JIANGSHIDAO_SELFTEST") != null) {
         ServerLifecycleEvents.SERVER_STARTED.register(SelfTest::run);
      }
   }

   private static void run(MinecraftServer server) {
      try {
         List<Item> items = new ArrayList<>();
         BuiltInRegistries.ITEM.stream().filter(i -> id(i).getNamespace().equals(JiangshiDao.MOD_ID)).forEach(items::add);
         List<Block> blocks = new ArrayList<>();
         BuiltInRegistries.BLOCK.stream().filter(b -> id(b).getNamespace().equals(JiangshiDao.MOD_ID)).forEach(blocks::add);
         long entities = BuiltInRegistries.ENTITY_TYPE.stream().filter(e -> id(e).getNamespace().equals(JiangshiDao.MOD_ID)).count();
         long menus = BuiltInRegistries.MENU.stream().filter(m -> id(m).getNamespace().equals(JiangshiDao.MOD_ID)).count();
         long effects = BuiltInRegistries.MOB_EFFECT.stream().filter(e -> id(e).getNamespace().equals(JiangshiDao.MOD_ID)).count();
         long blockEntities = BuiltInRegistries.BLOCK_ENTITY_TYPE.stream().filter(b -> id(b).getNamespace().equals(JiangshiDao.MOD_ID)).count();
         log("=== 僵尸道 Fabric 自检开始 ===");
         log("物品 " + items.size() + " / 方块 " + blocks.size() + " / 实体 " + entities + " / 菜单 " + menus + " / 药水 " + effects + " / 方块实体 " + blockEntities);
         int badItems = 0;

         for (Item item : items) {
            try {
               ItemStack stack = new ItemStack(item);
               if (stack.isEmpty()) {
                  badItems++;
                  log("  物品为空: " + id(item));
               }
            } catch (Throwable t) {
               badItems++;
               log("  物品构造失败: " + id(item) + " -> " + t);
            }
         }

         log("物品可用: " + (items.size() - badItems) + "/" + items.size());
         int eggs = 0;
         int badEggs = 0;

         for (Item item : items) {
            if (item instanceof SpawnEggItem) {
               eggs++;

               try {
                  EntityType<?> type = SpawnEggItem.getType(new ItemStack(item));
                  if (type == null) {
                     badEggs++;
                     log("  刷怪蛋没有实体类型: " + id(item));
                  } else {
                     log("  刷怪蛋 " + id(item) + " -> " + BuiltInRegistries.ENTITY_TYPE.getKey(type));
                  }
               } catch (Throwable t) {
                  badEggs++;
                  log("  刷怪蛋反查失败: " + id(item) + " -> " + t);
               }
            }
         }

         log("刷怪蛋可用: " + (eggs - badEggs) + "/" + eggs);
         ServerLevel level = server.overworld();
         int badBlocks = 0;

         for (Block block : blocks) {
            try {
               block.defaultBlockState();
            } catch (Throwable t) {
               badBlocks++;
               log("  方块状态失败: " + id(block) + " -> " + t);
            }
         }

         log("方块可用: " + (blocks.size() - badBlocks) + "/" + blocks.size());
         BlockPos pos = new BlockPos(0, 120, 0);
         int placed = 0;

         for (Block block : blocks) {
            if (block.defaultBlockState().hasBlockEntity()) {
               BlockPos p = pos.offset(placed * 3, 0, 0);
               level.setBlockAndUpdate(p, block.defaultBlockState());
               boolean ok = level.getBlockState(p).getBlock() == block;
               log("  方块实体放置 " + id(block) + " -> " + (ok ? "OK" : "失败"));
               if (ok) {
                  placed++;
               }
            }
         }

         List<String> entityIds = new ArrayList<>();
         BuiltInRegistries.ENTITY_TYPE.stream().filter(e -> id(e).getNamespace().equals(JiangshiDao.MOD_ID)).forEach(e -> entityIds.add(id(e).toString()));
         int spawned = 0;

         for (String eid : entityIds) {
            BlockPos sp = new BlockPos(0, 120, 0);
            String cmd = "summon " + eid + " " + (sp.getX() + 4 + spawned * 2) + " " + (sp.getY() + 1) + " " + sp.getZ();

            try {
               server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd);
               spawned++;
            } catch (Throwable t) {
               log("  实体生成失败: " + eid + " -> " + t);
            }
         }

         log("实体生成: " + spawned + "/" + entityIds.size());
         long allowedInPeaceful = BuiltInRegistries.ENTITY_TYPE
            .stream()
            .filter(e -> id(e).getNamespace().equals(JiangshiDao.MOD_ID))
            .filter(e -> e.getCategory() == MobCategory.MONSTER)
            .filter(EntityType::isAllowedInPeaceful)
            .count();
         log("和平难度下仍存在的敌对实体: " + allowedInPeaceful + "（必须是 0）");
         log("=== 僵尸道 Fabric 自检结束 ===");
      } catch (Throwable t) {
         log("自检异常: " + t);
         t.printStackTrace();
         testChunyangJin(server);
      } finally {
         server.halt(false);
      }
   }

   /**
    * 纯阳巾专项自检：确认注册、组件、槽位、资源接线是否齐全。
    *
    * <p>这些都属于"出错也不会报异常、但功能不存在"的类型，
    * 放在自检里可以直接从启动日志看出来。
    */
   private static void testChunyangJin(MinecraftServer server) {
      log("--- 纯阳巾自检 ---");

      // 1. 物品是否注册
      Item item = ModItems.CHUNYANG_JIN.get();
      log("  物品注册: " + id(item));

      // 2. 数据组件是否可用（读不到就会空指针）
      ItemStack stack = new ItemStack(item);
      var component = ModDataComponents.CHUNYANG_WEAR.get();
      log("  计时组件: " + (component == null ? "缺失!" : "正常"));

      // 3. 初始时长为 0、未激活
      log("  初始时长=" + ChunyangJinItem.wearTicks(stack) + " 已激活=" + ChunyangJinItem.isActive(stack));

      // 4. 写入读回
      ChunyangJinItem.setWearTicks(stack, ChunyangJinItem.ACTIVATE_TICKS + 1);
      boolean ok = ChunyangJinItem.wearTicks(stack) == ChunyangJinItem.ACTIVATE_TICKS + 1
         && ChunyangJinItem.isActive(stack);
      log("  写入读回: " + (ok ? "正常" : "异常!"));
      log("  激活门槛=" + ChunyangJinItem.activateTicks() + " tick（" + ChunyangJinItem.ACTIVATE_DAYS + " 日）");
      log("  显形半径=" + ChunyangJinItem.REVEAL_RADIUS + " 格");

      // 5. 头部槽是否接受它
      boolean accepted = AccessorySlot.HEAD.accepts(stack);
      log("  头部槽接受: " + (accepted ? "是" : "否!"));
      log("  其他槽拒绝: " + (!AccessorySlot.BACK.accepts(stack) && !AccessorySlot.WAIST.accepts(stack)
         && !AccessorySlot.LEGS.accepts(stack) ? "是" : "否!"));

      // 6. 配方是否加载
      int recipeCount = server.getRecipeManager()
         .byKey(net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.RECIPE, JiangshiDao.id("chunyang_jin")))
         .map(r -> 1)
         .orElse(0);
      log("  合成配方: " + (recipeCount > 0 ? "已加载" : "缺失!"));
   }

   private static Identifier id(Object o) {
      if (o instanceof Item i) {
         return BuiltInRegistries.ITEM.getKey(i);
      } else if (o instanceof Block b) {
         return BuiltInRegistries.BLOCK.getKey(b);
      } else if (o instanceof EntityType<?> e) {
         return BuiltInRegistries.ENTITY_TYPE.getKey(e);
      } else if (o instanceof MenuType<?> m) {
         return BuiltInRegistries.MENU.getKey(m);
      } else if (o instanceof MobEffect e) {
         return BuiltInRegistries.MOB_EFFECT.getKey(e);
      } else {
         return o instanceof BlockEntityType<?> b ? BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(b) : JiangshiDao.id("unknown");
      }
   }

   private static void log(String msg) {
      JiangshiDao.LOGGER.info("[自检] " + msg);
   }

   private SelfTest() {
   }
}