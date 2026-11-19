package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/**
 * ModRenderers。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ModRenderers {
   private ModRenderers() {
   }

   public static void registerModelLayers() {
      ModelLayerRegistry.registerModelLayer(JiangshiRenderer.LAYER_NORMAL, JiangshiModel::createBodyLayer);
      ModelLayerRegistry.registerModelLayer(JiangshiRenderer.LAYER_BRUTE, JiangshiModel::createBruteLayer);
      ModelLayerRegistry.registerModelLayer(GhostWomanRenderer.LAYER, GhostWomanModel::createBodyLayer);
      JiangshiDao.LOGGER
         .info("[渲染] 模型层注册完成：{} / {} / {}（贴图均为 128x128）", new Object[]{JiangshiRenderer.LAYER_NORMAL, JiangshiRenderer.LAYER_BRUTE, GhostWomanRenderer.LAYER});
   }
}
