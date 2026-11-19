package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.entity.GhostWomanVariant;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * GhostWomanRenderState。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class GhostWomanRenderState extends LivingEntityRenderState {
   public GhostWomanVariant variant = GhostWomanVariant.BLUE;
   public boolean airborne;
   public float horizontalSpeed;
}
