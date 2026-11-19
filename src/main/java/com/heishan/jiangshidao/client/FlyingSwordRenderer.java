package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.entity.FlyingSwordEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * FlyingSwordRenderer —— 客户端渲染器。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class FlyingSwordRenderer extends EntityRenderer<FlyingSwordEntity, FlyingSwordRenderState> {
   private final ItemModelResolver itemModelResolver;

   public FlyingSwordRenderer(Context context) {
      super(context);
      this.itemModelResolver = context.getItemModelResolver();
      JiangshiDao.LOGGER.info("[渲染] 飞剑渲染器已启用（按速度定向 + 物品模型几何）");
   }

   public FlyingSwordRenderState createRenderState() {
      return new FlyingSwordRenderState();
   }

   public void extractRenderState(FlyingSwordEntity entity, FlyingSwordRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      Vec3 vel = entity.getDeltaMovement();
      if (vel.lengthSqr() > 1.0E-6) {
         state.yaw = (float)(Mth.atan2(vel.z, vel.x) * (180.0 / Math.PI)) - 90.0F;
         double horiz = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
         state.pitch = (float)(-Mth.atan2(vel.y, horiz) * (180.0 / Math.PI));
      } else {
         state.yaw = entity.getYRot(partialTick);
         state.pitch = 0.0F;
      }

      this.itemModelResolver.updateForNonLiving(state.item, new ItemStack(entity.swordItem()), ItemDisplayContext.FIXED, entity);
   }

   public void submit(FlyingSwordRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      poseStack.pushPose();
      poseStack.rotateDegrees(Axis.YP, -state.yaw);
      poseStack.rotateDegrees(Axis.XP, state.pitch);
      poseStack.rotateDegrees(Axis.XP, 90.0F);
      poseStack.scale(0.85F, 0.85F, 0.85F);
      state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
      poseStack.popPose();
      super.submit(state, poseStack, collector, camera);
   }
}
