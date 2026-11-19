package com.heishan.jiangshidao.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * QuiverRenderer —— 客户端渲染器。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class QuiverRenderer extends RenderLayer<AvatarRenderState, PlayerModel> {
   private static final float SCALE = 1.5F;
   private static final float ITEM_FIXED_SCALE = 0.5F;
   private static final double SWORD_LIFT = 0.42;
   private static final float SWORD_SCALE = 0.56F;
   private static final double UNIT = 0.046875;
   private static final double PIVOT_SHIFT = 0.3046875;
   private static final double BOTTOM_Y = 0.6;
   private static final double CENTER_Y = 0.9046875;
   private static final double BACK_Z = 0.16;
   private static final int SHEATH_COUNT = 3;
   private static final float FAN_DEGREES = 30.0F;
   private static final double MODEL_Y_OFFSET = 1.501;
   private static final float PLAYER_SCALE = 0.9375F;

   public QuiverRenderer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
      super(parent);
   }

   public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, AvatarRenderState state, float yRot, float xRot) {
      if (!state.isInvisible) {
         QuiverRenderData data = (QuiverRenderData)((FabricRenderState)state).getData(ClientRenderData.QUIVER);
         if (data != null) {
            poseStack.pushPose();
            poseStack.translate(0.0, 1.501, 0.0);
            float invScale = 1.0666667F;
            poseStack.scale(invScale, invScale, invScale);
            poseStack.scale(-1.0F, -1.0F, 1.0F);

            for (int i = 0; i < 3; i++) {
               float tilt = (i - 1) * 30.0F;
               double spread = (i - 1) * 0.045;
               poseStack.pushPose();
               poseStack.translate(spread, 0.9046875, 0.16);
               poseStack.rotateDegrees(Axis.ZP, tilt);
               poseStack.translate(0.0, 0.3046875, 0.0);
               poseStack.scale(1.5F, 1.5F, 1.5F);
               data.quiver.submit(poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, state.outlineColor);
               if (data.hasSword(i)) {
                  poseStack.pushPose();
                  poseStack.translate(0.0, 0.42, 0.0);
                  poseStack.scale(0.56F, 0.56F, 0.56F);
                  data.swords[i].submit(poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, state.outlineColor);
                  poseStack.popPose();
               }

               poseStack.popPose();
            }

            poseStack.popPose();
         }
      }
   }
}
