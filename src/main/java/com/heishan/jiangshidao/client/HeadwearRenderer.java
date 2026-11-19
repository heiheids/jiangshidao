package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * HeadwearRenderer —— 把头部饰品（纯阳巾）画在玩家头顶。
 *
 * <p><b>为什么绑定头部模型而不是用固定高度：</b>
 * 之前用固定 y 值定位，结果无论怎么调都落在腰部或头部内部 ——
 * 因为渲染层拿到的坐标基准与"世界高度"不是一回事。
 *
 * <p>现在改成直接跟随玩家模型的 {@code head} 部件：
 * 先 {@code head.translateAndRotate(poseStack)} 把坐标系搬到头部（含头部俯仰），
 * 再上移到头顶。这样无论玩家姿势、潜行、坐骑，道冠都牢牢贴在头上。
 *
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class HeadwearRenderer extends RenderLayer<AvatarRenderState, PlayerModel> {
   /**
    * 冠底相对头部原点的上移量（模型像素，1 = 1/16 格）。
    *
    * <p><b>这里必须是负值。</b>实测结论：
    * 渲染层的 y 轴方向与世界相反 —— 传入正值会向下移动。
    * <ul>
    *   <li>传 {@code +0.3125} 时道冠出现在腰部（世界约 1.19 格）→ 验证了"正值向下"</li>
    *   <li>传 {@code +1.95} 时道冠沉到地面以下，所以"头顶什么都没有"</li>
    * </ul>
    *
    * <p>换算关系：{@code 世界高度 = 1.5 - 输入值}，玩家头顶 2.0 格 ⇒ 输入 -0.5。
    * 这里用 {@link #EXTRA_LIFT} 做微调，默认 -0.5 正好落在头顶。
    */
   private static final double LIFT_PIXELS = 0.0;

   /**
    * 模型缩放（可用启动参数微调：{@code -Djiangshidao.headwear.scale=0.8}）。
    */
   private static final float SCALE = (float)tuneDouble("jiangshidao.headwear.scale", 0.385);

   /**
    * 冠底高度微调（格）。负值向上、正值向下。
    *
    * <p>实测 -0.5 时冠底约在 1.6 格（陷进头里，只露出一点金边），再降 0.4 得到 -0.9。
    * 可用 {@code -Djiangshidao.headwear.y} 覆盖：更负=更高，更正=更低。
    */
   private static final double EXTRA_LIFT = tuneDouble("jiangshidao.headwear.y", -0.525);

   public HeadwearRenderer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
      super(parent);
      JiangshiDao.LOGGER.info(
         "[渲染] 纯阳巾头部渲染参数：lift={}px extra={} scale={}（可用 -Djiangshidao.headwear.y / -Djiangshidao.headwear.scale 覆盖）",
         new Object[]{LIFT_PIXELS, EXTRA_LIFT, SCALE}
      );
   }

   private static double tuneDouble(String key, double fallback) {
      try {
         String raw = System.getProperty(key);
         if (raw != null && !raw.isBlank()) {
            return Double.parseDouble(raw.trim());
         }
      } catch (Throwable ignored) {
         // 参数写错用默认值
      }

      return fallback;
   }

   public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, AvatarRenderState state, float yRot, float xRot) {
      HeadwearRenderData data = (HeadwearRenderData)((FabricRenderState)state).getData(ClientRenderData.HEADWEAR);
      if (data == null || state.isInvisible) {
         return;
      }

      PlayerModel model = this.getParentModel();

      poseStack.pushPose();
      // 1) 跟随头部：坐标系移到头部原点，并套用头部俯仰
      this.getParentModel().head.translateAndRotate(poseStack);
      // 2) 上移到头顶（模型像素 → 格），再叠加粗调
      poseStack.translate(0.0, LIFT_PIXELS / 16.0 + EXTRA_LIFT, 0.0);
      // 3) 与剑囊渲染层一致的坐标翻转
      poseStack.scale(-1.0F, -1.0F, 1.0F);
      poseStack.scale(SCALE, SCALE, SCALE);

      data.model.submit(poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, state.outlineColor);
      poseStack.popPose();
   }
}
