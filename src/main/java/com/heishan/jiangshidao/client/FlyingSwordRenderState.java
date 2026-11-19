package com.heishan.jiangshidao.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * FlyingSwordRenderState。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class FlyingSwordRenderState extends EntityRenderState {
   public final ItemStackRenderState item = new ItemStackRenderState();
   public float yaw;
   public float pitch;
}
