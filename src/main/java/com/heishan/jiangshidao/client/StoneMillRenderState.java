package com.heishan.jiangshidao.client;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/**
 * StoneMillRenderState。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class StoneMillRenderState extends BlockEntityRenderState {
   public final BlockModelRenderState upper = new BlockModelRenderState();
   public float angle;
}
