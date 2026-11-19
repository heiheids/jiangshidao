package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.item.TalismanPouchItem;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterExtract;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterInit;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * PouchOverlay —— HUD 覆盖层绘制。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class PouchOverlay {
   public static final int SLOT_SIZE = 18;
   private static final int MARGIN_LEFT = 2;
   private static final Identifier ELEMENT_ID = JiangshiDao.id("pouch_slots");
   private static boolean reportedError;
   private static boolean reportedFirstDraw;

   private PouchOverlay() {
   }

   public static boolean isPouchScreen(Screen screen) {
      return screen instanceof AbstractRecipeBookScreen || screen instanceof CreativeModeInventoryScreen;
   }

   public static void register() {
      HudElementRegistry.addLast(ELEMENT_ID, PouchOverlay::extract);
      JiangshiDao.LOGGER.info("[符袋] HUD 元素已注册：{}", ELEMENT_ID);
      ScreenEvents.AFTER_INIT.register((AfterInit)(client, screen, scaledWidth, scaledHeight) -> {
         if (isPouchScreen(screen)) {
            ScreenEvents.afterExtract(screen).register((AfterExtract)(s, graphics, mouseX, mouseY, tickDelta) -> {
               try {
                  renderPanel(graphics);
                  renderHoverTooltip(graphics, mouseX, mouseY);
                  drawCarriedOnTop(graphics, mouseX, mouseY);
               } catch (Throwable t) {
                  reportOnce("[符袋] 背包界面内绘制符袋面板失败", t);
               }
            });
         }
      });
   }

   public static int panelX() {
      return 2;
   }

   public static int panelY(int screenHeight) {
      int total = TalismanPouchItem.slotCount() * 18;
      return Math.max(2, (screenHeight - total) / 2);
   }

   private static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null || mc.level == null) {
            return;
         }

         if (mc.gui != null && mc.gui.screen() != null) {
            return;
         }

         renderPanel(graphics);
      } catch (Throwable t) {
         reportOnce("[符袋] HUD 渲染失败", t);
      }
   }

   private static void renderHoverTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player != null) {
         ItemStack pouch = TalismanPouchItem.findPouch(player);
         if (!pouch.isEmpty()) {
            List<ItemStack> slots = TalismanPouchItem.getSlots(pouch);
            int x = panelX();
            int y0 = panelY(graphics.guiHeight());

            for (int i = 0; i < slots.size(); i++) {
               int y = y0 + i * 18;
               if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                  ItemStack content = slots.get(i);
                  if (!content.isEmpty()) {
                     graphics.setTooltipForNextFrame(mc.font, content, mouseX, mouseY);
                  }

                  return;
               }
            }
         }
      }
   }

   private static void drawCarriedOnTop(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player != null && player.containerMenu != null) {
         ItemStack carried = player.containerMenu.getCarried();
         if (!carried.isEmpty()) {
            int itemX = mouseX - 8;
            int itemY = mouseY - 8;
            if (overlapsPanel(itemX, itemY, 16, 16, graphics.guiHeight())) {
               graphics.item(carried, itemX, itemY);
               graphics.itemDecorations(mc.font, carried, itemX, itemY);
            }
         }
      }
   }

   private static boolean overlapsPanel(int x, int y, int width, int height, int screenHeight) {
      int px = panelX();
      int py = panelY(screenHeight);
      int panelWidth = 18;
      int panelHeight = TalismanPouchItem.slotCount() * 18;
      return x < px + panelWidth && x + width > px && y < py + panelHeight && y + height > py;
   }

   static void renderPanel(GuiGraphicsExtractor graphics) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player != null && mc.level != null) {
         ItemStack pouch = TalismanPouchItem.findPouch(player);
         if (pouch.isEmpty()) {
            PouchSelection.reset();
         } else {
            List<ItemStack> slots = TalismanPouchItem.getSlots(pouch);
            int x = panelX();
            int y0 = panelY(graphics.guiHeight());
            int selected = PouchSelection.selected();

            for (int i = 0; i < slots.size(); i++) {
               int y = y0 + i * 18;
               drawSlot(graphics, x, y, i == selected);
               ItemStack content = slots.get(i);
               if (!content.isEmpty()) {
                  graphics.item(content, x + 1, y + 1);
                  graphics.itemDecorations(mc.font, content, x + 1, y + 1);
               }
            }

            if (!reportedFirstDraw) {
               reportedFirstDraw = true;
               JiangshiDao.LOGGER.info("[符袋] HUD 首次绘制：{} 格，选中槽位 {}，面板位置 ({}, {})", new Object[]{slots.size(), selected, x, y0});
            }
         }
      }
   }

   private static void drawSlot(GuiGraphicsExtractor graphics, int x, int y, boolean selected) {
      int border = selected ? -1 : -7631989;
      graphics.fill(x, y, x + 18, y + 18, border);
      graphics.fill(x + 1, y + 1, x + 18 - 1, y + 18 - 1, -13158601);
      if (selected) {
         graphics.fill(x + 1, y + 1, x + 18 - 1, y + 18 - 1, 1090508544);
      } else {
         graphics.fill(x + 1, y + 1, x + 18 - 1, y + 2, -14013910);
         graphics.fill(x + 1, y + 1, x + 2, y + 18 - 1, -14013910);
      }
   }

   private static void reportOnce(String message, Throwable t) {
      if (!reportedError) {
         reportedError = true;
         JiangshiDao.LOGGER.error(message + "（后续不再重复记录）", t);
      }
   }
}
