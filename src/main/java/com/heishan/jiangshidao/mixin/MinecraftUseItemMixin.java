package com.heishan.jiangshidao.mixin;

import com.heishan.jiangshidao.client.PouchInputHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
/**
 * MinecraftUseItemMixin —— 混入实现，改写原版行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class MinecraftUseItemMixin {
   @Shadow
   private int rightClickDelay;

   @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
   private void jiangshidao$startUseItem(CallbackInfo ci) {
      try {
         if (PouchInputHandler.onUseKey()) {
            this.rightClickDelay = 4;
            ci.cancel();
         }
      } catch (Throwable error) {
      }
   }
}
