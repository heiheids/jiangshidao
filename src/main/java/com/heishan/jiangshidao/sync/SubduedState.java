package com.heishan.jiangshidao.sync;

import net.minecraft.world.entity.LivingEntity;

/**
 * SubduedState。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public interface SubduedState {
   boolean jiangshidao$isSubdued();

   static boolean of(LivingEntity entity) {
      return entity instanceof SubduedState state && state.jiangshidao$isSubdued();
   }
}
