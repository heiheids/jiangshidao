package com.heishan.jiangshidao.network;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.compat.Net;
import com.heishan.jiangshidao.item.TalismanPouchItem;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

/**
 * PouchQuickMoveHandler —— 事件/任务处理。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class PouchQuickMoveHandler {
   private PouchQuickMoveHandler() {
   }

   public static void register() {
      Net.c2s(PouchQuickMovePayload.TYPE, PouchQuickMovePayload.STREAM_CODEC, (payload, context) -> {
         ServerPlayer player = context.player();
         if (player != null) {
            try {
               quickMove(player, payload.pouchSlot(), payload.inventorySlot());
            } catch (Throwable t) {
               JiangshiDao.LOGGER.error("[符袋] 快速移动失败", t);
            }
         }
      });
      JiangshiDao.LOGGER.info("[符袋] 快捷移动（Shift+左键）服务端已接入");
   }

   private static void quickMove(ServerPlayer player, int pouchSlot, int inventorySlot) {
      if (!player.level().isClientSide()) {
         ItemStack pouch = TalismanPouchItem.findPouch(player);
         if (!pouch.isEmpty()) {
            boolean changed = pouchSlot >= 0 ? pouchToInventory(player, pouch, pouchSlot) : inventoryToPouch(player, pouch, inventorySlot);
            if (changed) {
               sync(player, pouch);
            }
         }
      }
   }

   private static boolean pouchToInventory(ServerPlayer player, ItemStack pouch, int pouchSlot) {
      if (pouchSlot >= 0 && pouchSlot < TalismanPouchItem.slotCount()) {
         List<ItemStack> slots = TalismanPouchItem.getSlots(pouch);
         ItemStack inSlot = slots.get(pouchSlot);
         if (inSlot.isEmpty()) {
            return false;
         } else {
            ItemStack moving = inSlot.copy();
            player.getInventory().add(moving);
            if (moving.getCount() == inSlot.getCount()) {
               JiangshiDao.LOGGER.debug("[符袋] 背包已满，槽位 {} 的符留在符袋里", pouchSlot);
               return false;
            } else {
               slots.set(pouchSlot, moving.isEmpty() ? ItemStack.EMPTY : moving);
               TalismanPouchItem.setSlots(pouch, slots);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private static boolean inventoryToPouch(ServerPlayer player, ItemStack pouch, int inventorySlot) {
      if (inventorySlot >= 0 && inventorySlot < player.getInventory().getContainerSize()) {
         ItemStack src = player.getInventory().getItem(inventorySlot);
         if (!TalismanPouchItem.isValidContent(src)) {
            return false;
         }

         if (!TalismanPouchItem.tryInsert(pouch, src)) {
            return false;
         }

         if (src.isEmpty()) {
            player.getInventory().setItem(inventorySlot, ItemStack.EMPTY);
         }

         return true;
      } else {
         return false;
      }
   }

   private static void sync(ServerPlayer player, ItemStack pouch) {
      List<ItemStack> slots = TalismanPouchItem.getSlots(pouch);
      ModNetwork.sendTo(player, new PouchSyncPayload(slots));
      if (player.containerMenu instanceof InventoryMenu menu) {
         menu.broadcastFullState();
      }

      player.containerMenu.broadcastChanges();
      player.getInventory().setChanged();
   }
}
