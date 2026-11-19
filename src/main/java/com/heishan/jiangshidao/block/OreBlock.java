package com.heishan.jiangshidao.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * OreBlock —— 方块定义与交互行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class OreBlock extends Block {
   public OreBlock(String name, MapColor color) {
      super(BlockProps.of(name).mapColor(color).strength(3.0F, 3.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
   }
}
