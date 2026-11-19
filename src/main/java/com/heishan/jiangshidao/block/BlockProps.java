package com.heishan.jiangshidao.block;

import com.heishan.jiangshidao.JiangshiDao;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

/**
 * BlockProps。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class BlockProps {
   private BlockProps() {
   }

   public static Properties of(String name) {
      return Properties.of().setId(ResourceKey.create(Registries.BLOCK, JiangshiDao.id(name)));
   }

   public static Properties copyOf(BlockBehaviour source, String name) {
      return Properties.ofFullCopy(source).setId(ResourceKey.create(Registries.BLOCK, JiangshiDao.id(name)));
   }
}
