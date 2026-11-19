package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.mixin.ContainerScreenAccess;
import com.heishan.jiangshidao.network.OpenAccessoryPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * AccessoryButton。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class AccessoryButton extends AbstractWidget {
   private static final Identifier SPRITE = JiangshiDao.id("accessory_button");
   private static final int SIZE = 12;
   private static final int OFFSET_X = 26;
   private static final int OFFSET_Y = 62;
   private final InventoryScreen owner;

   public AccessoryButton(InventoryScreen owner) {
      super(0, 0, 12, 12, Component.translatable("button.jiangshidao.accessories"));
      this.owner = owner;
   }

   protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      ContainerScreenAccess access = (ContainerScreenAccess)this.owner;
      this.setX(access.jiangshidao$guiLeft() + 26);
      this.setY(access.jiangshidao$guiTop() + 62);
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE, this.getX(), this.getY(), 12, 12);
      if (this.isHovered()) {
         graphics.fill(RenderPipelines.GUI, this.getX(), this.getY(), this.getX() + 12, this.getY() + 12, 1358954495);
      }
   }

   public void onClick(MouseButtonEvent event, boolean doubleClick) {
      try {
         if (ClientPlayNetworking.canSend(OpenAccessoryPayload.TYPE)) {
            ClientPlayNetworking.send(new OpenAccessoryPayload());
         } else {
            JiangshiDao.LOGGER.warn("[饰品栏] 服务端没有登记 open_accessory 包，忽略本次点击");
         }
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[饰品栏] 打开饰品栏的发包失败", t);
      }
   }

   protected void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }
}
