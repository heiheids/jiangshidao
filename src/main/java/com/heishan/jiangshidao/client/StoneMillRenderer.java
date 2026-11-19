package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.block.StoneMillBlock;
import com.heishan.jiangshidao.block.StoneMillBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * StoneMillRenderer —— 客户端渲染器。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class StoneMillRenderer implements BlockEntityRenderer<StoneMillBlockEntity, StoneMillRenderState> {
   private final BlockModelResolver blockModelResolver;
   private static boolean LOGGED;

   public StoneMillRenderer(Context context) {
      this.blockModelResolver = context.blockModelResolver();
      JiangshiDao.LOGGER.info("[渲染] 石磨渲染器已启用（上扇绕 Y 轴旋转，26.3 模型解析版）");
   }

   public StoneMillRenderState createRenderState() {
      return new StoneMillRenderState();
   }

   public void extractRenderState(StoneMillBlockEntity be, StoneMillRenderState state, float partialTick, Vec3 cameraPos, CrumblingOverlay crumbling) {
      BlockEntityRenderState.extractBase(be, state, crumbling);
      state.angle = be.currentAngle();
      BlockState upperState = (BlockState)be.getBlockState().setValue(StoneMillBlock.UPPER, Boolean.TRUE);
      this.blockModelResolver.update(state.upper, upperState, BlockDisplayContext.create());
      if (!LOGGED) {
         LOGGED = true;
         JiangshiDao.LOGGER.info("[渲染] 石磨渲染状态抽取完成：upper 模型为空={}，角度={}", state.upper.isEmpty(), state.angle);
      }
   }

   public void submit(StoneMillRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      if (!state.upper.isEmpty()) {
         poseStack.pushPose();
         poseStack.translate(0.5, 0.0, 0.5);
         poseStack.rotateDegrees(Axis.YP, state.angle);
         poseStack.translate(-0.5, 0.0, -0.5);
         state.upper.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
         poseStack.popPose();
      }
   }
}
