package com.heishan.jiangshidao.client;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

/**
 * ClientRenderData —— 数据模型。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ClientRenderData {
   public static final RenderStateDataKey<Boolean> SUBDUED = RenderStateDataKey.create(() -> "jiangshidao:subdued");
   public static final RenderStateDataKey<QuiverRenderData> QUIVER = RenderStateDataKey.create(() -> "jiangshidao:quiver");
   /** 头部饰品（纯阳巾）的渲染数据。 */
   public static final RenderStateDataKey<HeadwearRenderData> HEADWEAR = RenderStateDataKey.create(() -> "jiangshidao:headwear");

   private ClientRenderData() {
   }
}
