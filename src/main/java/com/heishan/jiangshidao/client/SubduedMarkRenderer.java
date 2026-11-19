package com.heishan.jiangshidao.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * SubduedMarkRenderer —— 客户端渲染器。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class SubduedMarkRenderer {
   private static final FormattedCharSequence MARK = Component.literal("?").getVisualOrderText();
   private static final int COLOR_OUTLINE = -2842601;
   private static final int COLOR_INNER = -12965376;

   private SubduedMarkRenderer() {
   }

   public static void render(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      Boolean subdued = (Boolean)((FabricRenderState)state).getData(ClientRenderData.SUBDUED);
      if (subdued != null && subdued) {
         if (camera != null && camera.orientation != null) {
            Font font = Minecraft.getInstance().font;
            float width = font.width(MARK) / 2.0F;
            poseStack.pushPose();
            poseStack.translate(0.0, state.boundingBoxHeight + 1.15, 0.0);
            poseStack.rotate(camera.orientation);
            poseStack.scale(-0.05F, -0.05F, 0.05F);
            collector.submitText(poseStack, -width, 0.0F, MARK, false, DisplayMode.SEE_THROUGH, state.lightCoords, -2842601, 0, 0);
            collector.submitText(poseStack, -width, 0.0F, MARK, false, DisplayMode.NORMAL, state.lightCoords, -12965376, 0, 0);
            poseStack.popPose();
         }
      }
   }
}
