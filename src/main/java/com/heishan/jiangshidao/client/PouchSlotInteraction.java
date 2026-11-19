package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.item.TalismanPouchItem;
import com.heishan.jiangshidao.mixin.ContainerScreenAccess;
import com.heishan.jiangshidao.network.PouchContentsPayload;
import com.heishan.jiangshidao.network.PouchQuickMovePayload;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterInit;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents.AllowMouseClick;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * PouchSlotInteraction。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class PouchSlotInteraction {
   private static final int SLOT_HIT_SIZE = 16;

   private PouchSlotInteraction() {
   }

   public static void register() {
      ScreenEvents.AFTER_INIT.register((AfterInit)(client, screen, scaledWidth, scaledHeight) -> {
         if (PouchOverlay.isPouchScreen(screen)) {
            ScreenMouseEvents.allowMouseClick(screen).register((AllowMouseClick)(s, event) -> {
               try {
                  return !handleClick(s, event);
               } catch (Throwable t) {
                  JiangshiDao.LOGGER.error("[符袋] 处理槽位点击失败", t);
                  return true;
               }
            });
         }
      });
      JiangshiDao.LOGGER.info("[符袋] 背包界面槽位点击已接入（含 Shift 快捷移动）");
   }

   private static boolean handleClick(Screen screen, MouseButtonEvent event) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player == null) {
         return false;
      }

      ItemStack pouch = TalismanPouchItem.findPouch(player);
      if (pouch.isEmpty()) {
         return false;
      }

      double mouseX = event.x();
      double mouseY = event.y();
      boolean shift = isShiftDown();
      int x = PouchOverlay.panelX();
      int y0 = PouchOverlay.panelY(screen.height);

      for (int i = 0; i < TalismanPouchItem.slotCount(); i++) {
         int y = y0 + i * 18;
         boolean inside = mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
         if (inside) {
            if (!shift) {
               int button = event.button() == 3 ? 1 : 0;
               normalClick(player, pouch, i, button);
            } else if (!TalismanPouchItem.getSlots(pouch).get(i).isEmpty()) {
               sendQuickMove(i, -1);
            }

            return true;
         }
      }

      if (shift) {
         int invSlot = hitPlayerInventorySlot(screen, player, mouseX, mouseY);
         if (invSlot >= 0 && canQuickMoveIntoPouch(player, pouch, invSlot)) {
            sendQuickMove(-1, invSlot);
            return true;
         }
      }

      return false;
   }

   private static void normalClick(Player player, ItemStack pouch, int index, int button) {
      ItemStack carried = player.containerMenu.getCarried();
      ItemStack newCarried = TalismanPouchItem.applyClick(pouch, index, button, carried);
      player.containerMenu.setCarried(newCarried);
      List<ItemStack> slots = TalismanPouchItem.getSlots(pouch);
      if (ClientPlayNetworking.canSend(PouchContentsPayload.TYPE)) {
         ClientPlayNetworking.send(new PouchContentsPayload(slots, newCarried));
      } else {
         JiangshiDao.LOGGER.warn("[符袋] 服务端没有登记 pouch_contents 包，槽位改动未提交");
      }
   }

   private static void sendQuickMove(int pouchSlot, int inventorySlot) {
      if (ClientPlayNetworking.canSend(PouchQuickMovePayload.TYPE)) {
         ClientPlayNetworking.send(new PouchQuickMovePayload(pouchSlot, inventorySlot));
      } else {
         JiangshiDao.LOGGER.warn("[符袋] 服务端没有登记 pouch_quick_move 包，本次快捷移动被忽略");
      }
   }

   private static boolean canQuickMoveIntoPouch(Player player, ItemStack pouch, int inventorySlot) {
      if (inventorySlot >= 0 && inventorySlot < player.getInventory().getContainerSize()) {
         ItemStack src = player.getInventory().getItem(inventorySlot);
         if (!TalismanPouchItem.isValidContent(src)) {
            return false;
         }

         for (ItemStack s : TalismanPouchItem.getSlots(pouch)) {
            if (s.isEmpty()) {
               return true;
            }

            if (ItemStack.isSameItemSameComponents(s, src) && s.getCount() < s.getMaxStackSize()) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static int hitPlayerInventorySlot(Screen screen, Player player, double mouseX, double mouseY) {
      if (screen instanceof AbstractContainerScreen<?> containerScreen) {
         if (screen instanceof ContainerScreenAccess access) {
            int left = access.jiangshidao$guiLeft();
            int top = access.jiangshidao$guiTop();

            for (Slot slot : containerScreen.getMenu().slots) {
               if (slot.container == player.getInventory()) {
                  int sx = left + slot.x;
                  int sy = top + slot.y;
                  if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                     return slot.getContainerSlot();
                  }
               }
            }

            return -1;
         } else {
            return -1;
         }
      } else {
         return -1;
      }
   }

   private static boolean isShiftDown() {
      return InputConstants.isKeyDown(225) || InputConstants.isKeyDown(229);
   }
}
