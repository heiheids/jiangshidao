package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.accessory.AccessorySlot;
import com.heishan.jiangshidao.item.BronzeBellItem;
import com.heishan.jiangshidao.menu.AccessoryMenu;
import com.heishan.jiangshidao.network.WorkerPayloads;
import com.heishan.jiangshidao.registry.ModDataComponents;
import com.heishan.jiangshidao.worker.BellMode;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * AccessoryContainerScreen —— 客户端界面。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class AccessoryContainerScreen extends AbstractContainerScreen<AccessoryMenu> {
   private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
   private static final int PANEL_H = 232;
   private static final int BELL_BUTTON_W = 66;
   private static final int BELL_BUTTON_H = 18;
   private static final int BELL_BUTTON_GAP = 22;
   private static final int BELL_BUTTON_MAX_W = 120;
   private Button bellModeButton;
   private BellMode pendingBellMode;

   public AccessoryContainerScreen(AccessoryMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, 176, 232);
      this.titleLabelX = 8;
      this.titleLabelY = 6;
      this.inventoryLabelX = 8;
      this.inventoryLabelY = 134;
   }

   protected void init() {
      super.init();
      int bx = this.leftPos + 104;
      int by = this.topPos + AccessoryMenu.ACCESSORY_Y[AccessorySlot.LEGS.ordinal()] + 22;
      this.bellModeButton = Button.builder(Component.empty(), b -> this.cycleBellMode()).bounds(bx, by, 66, 18).build();
      this.addRenderableWidget(this.bellModeButton);
      this.refreshBellModeButton();
   }

   private boolean hasLegsBell() {
      ItemStack bell = ((AccessoryMenu)this.menu).legsAccessory();
      return bell.getItem() instanceof BronzeBellItem;
   }

   private BellMode bellMode() {
      ItemStack bell = ((AccessoryMenu)this.menu).legsAccessory();
      if (!(bell.getItem() instanceof BronzeBellItem)) {
         this.pendingBellMode = null;
         return BellMode.AGGRESSIVE;
      }

      BellMode actual = BellMode.byCode((Integer)bell.getOrDefault(ModDataComponents.BELL_MODE.get(), 1));
      if (this.pendingBellMode != null) {
         if (this.pendingBellMode != actual) {
            return this.pendingBellMode;
         }

         this.pendingBellMode = null;
      }

      return actual;
   }

   private void cycleBellMode() {
      if (this.hasLegsBell()) {
         BellMode next = this.bellMode().next();
         this.pendingBellMode = next;
         if (ClientPlayNetworking.canSend(WorkerPayloads.SetBellMode.TYPE)) {
            ClientPlayNetworking.send(new WorkerPayloads.SetBellMode(next.code()));
         } else {
            this.pendingBellMode = null;
            JiangshiDao.LOGGER.warn("[铜铃] 服务端没有登记 set_bell_mode 包，本次模式切换未提交");
         }

         this.refreshBellModeButton();
      }
   }

   private void refreshBellModeButton() {
      if (this.bellModeButton != null) {
         boolean hasBell = this.hasLegsBell();
         this.bellModeButton.visible = hasBell;
         this.bellModeButton.active = hasBell;
         if (hasBell) {
            Component label = Component.translatable("gui.jiangshidao.bellmode", new Object[]{this.bellMode().displayName()});
            this.bellModeButton.setMessage(label);
            int width = this.font.width(label) + 12;
            if (width < 66) {
               width = 66;
            }

            if (width > 120) {
               width = 120;
            }

            this.bellModeButton.setWidth(width);
            this.bellModeButton.setX(this.leftPos + this.imageWidth - 6 - width);
         }
      }
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractBackground(graphics, mouseX, mouseY, partialTick);
      this.refreshBellModeButton();
      int x = this.leftPos;
      int y = this.topPos;
      graphics.fill(RenderPipelines.GUI, x - 1, y - 1, x + this.imageWidth + 1, y + this.imageHeight + 1, -13948117);
      graphics.fill(RenderPipelines.GUI, x, y, x + this.imageWidth, y + this.imageHeight, -3750202);
      graphics.fill(RenderPipelines.GUI, x + 6, y + 146 - 16, x + this.imageWidth - 6, y + 146 - 15, -7631989);

      for (Slot slot : ((AccessoryMenu)this.menu).slots) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + slot.x - 1, y + slot.y - 1, 18, 18);
      }
   }

   protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      super.extractLabels(graphics, mouseX, mouseY);

      for (AccessorySlot slot : AccessorySlot.values()) {
         graphics.text(this.font, Component.translatable(slot.translationKey()), 126, AccessoryMenu.ACCESSORY_Y[slot.ordinal()] + 5, -12566464, false);
      }

      graphics.text(this.font, Component.translatable("accessory.jiangshidao.quiver"), 48, 25, -12566464, false);
      if (!this.hasLegsBell()) {
         graphics.text(this.font, Component.translatable("accessory.jiangshidao.hint"), 8, 110, -11184811, false);
         graphics.text(this.font, Component.translatable("accessory.jiangshidao.hint2"), 8, 120, -11184811, false);
      }
   }
}
