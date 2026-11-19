package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.entity.JiangshiEntity;
import com.heishan.jiangshidao.entity.JiangshiType;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * JiangshiRenderer —— 客户端渲染器。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class JiangshiRenderer extends HumanoidMobRenderer<JiangshiEntity, HumanoidRenderState, JiangshiModel> {
   public static final ModelLayerLocation LAYER_NORMAL = new ModelLayerLocation(JiangshiDao.id("jiangshi_normal"), "main");
   public static final ModelLayerLocation LAYER_BRUTE = new ModelLayerLocation(JiangshiDao.id("jiangshi_brute"), "main");
   private final Identifier texture;

   public static ModelLayerLocation layerFor(JiangshiType type) {
      return type.modelType() == JiangshiType.JiangshiModelType.BRUTE ? LAYER_BRUTE : LAYER_NORMAL;
   }

   public JiangshiRenderer(Context context, JiangshiType type) {
      super(context, new JiangshiModel(context.bakeLayer(layerFor(type)), type.modelType() == JiangshiType.JiangshiModelType.BRUTE), 0.5F);
      this.texture = JiangshiDao.id("textures/entity/" + type.id() + ".png");
      JiangshiDao.LOGGER.info("[渲染] 僵尸 {} 模型层烘焙成功：{}，贴图 {}（128x128 UV）", new Object[]{type.id(), layerFor(type), this.texture});
   }

   public HumanoidRenderState createRenderState() {
      return new HumanoidRenderState();
   }

   public Identifier getTextureLocation(HumanoidRenderState state) {
      return this.texture;
   }
}
