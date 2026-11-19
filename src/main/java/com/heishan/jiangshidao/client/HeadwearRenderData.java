package com.heishan.jiangshidao.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * HeadwearRenderData —— 头部饰品（纯阳巾）的渲染数据。
 *
 * <p>与 {@link QuiverRenderData} 同构：在渲染状态提取阶段把物品模型解析好，
 * 渲染层只负责摆放位置，避免每帧重复解析。
 *
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class HeadwearRenderData {
   public final ItemStackRenderState model = new ItemStackRenderState();

   public HeadwearRenderData() {
   }
}
