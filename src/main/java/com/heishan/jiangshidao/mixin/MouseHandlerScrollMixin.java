package com.heishan.jiangshidao.mixin;

import com.heishan.jiangshidao.client.PouchInputHandler;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
/**
 * MouseHandlerScrollMixin —— 混入实现，改写原版行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class MouseHandlerScrollMixin {
   @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
   private void jiangshidao$onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
      try {
         if (PouchInputHandler.onMouseScroll(yOffset)) {
            ci.cancel();
         }
      } catch (Throwable error) {
      }
   }
}
