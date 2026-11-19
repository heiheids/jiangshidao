package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.entity.GhostWomanEntity;
import com.heishan.jiangshidao.entity.GhostWomanVariant;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * GhostWomanRenderer —— 客户端渲染器。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class GhostWomanRenderer extends MobRenderer<GhostWomanEntity, GhostWomanRenderState, GhostWomanModel> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(JiangshiDao.id("ghost_woman"), "main");
   private static final Map<GhostWomanVariant, Identifier> TEXTURES = new EnumMap<>(GhostWomanVariant.class);

   public GhostWomanRenderer(Context context) {
      super(context, new GhostWomanModel(context.bakeLayer(LAYER)), 0.4F);
      JiangshiDao.LOGGER
         .info(
            "[渲染] 女鬼模型层烘焙成功：{}，贴图 {}/{}/{}（128x128 UV）",
            new Object[]{LAYER, TEXTURES.get(GhostWomanVariant.BLUE), TEXTURES.get(GhostWomanVariant.RED), TEXTURES.get(GhostWomanVariant.WHITE)}
         );
   }

   public GhostWomanRenderState createRenderState() {
      return new GhostWomanRenderState();
   }

   public void extractRenderState(GhostWomanEntity entity, GhostWomanRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      state.variant = entity.variant();
      state.airborne = !entity.onGround();
      Vec3 motion = entity.getDeltaMovement();
      state.horizontalSpeed = (float)Math.sqrt(motion.x * motion.x + motion.z * motion.z);
   }

   public Identifier getTextureLocation(GhostWomanRenderState state) {
      GhostWomanVariant variant = state.variant;
      Identifier loc = variant == null ? null : TEXTURES.get(variant);
      return loc != null ? loc : TEXTURES.get(GhostWomanVariant.BLUE);
   }

   static {
      for (GhostWomanVariant v : GhostWomanVariant.values()) {
         TEXTURES.put(v, JiangshiDao.id("textures/entity/" + v.textureFile() + ".png"));
      }
   }
}
