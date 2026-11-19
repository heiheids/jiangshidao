package com.heishan.jiangshidao.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
/**
 * ContainerScreenAccess。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public interface ContainerScreenAccess {
   @Accessor("leftPos")
   int jiangshidao$guiLeft();

   @Accessor("topPos")
   int jiangshidao$guiTop();
}
