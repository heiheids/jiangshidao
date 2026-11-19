package com.heishan.jiangshidao.mixin;

import com.heishan.jiangshidao.registry.ModEffects;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
/**
 * ZombieConversionMixin —— 混入实现，改写原版行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public abstract class ZombieConversionMixin {
   @Inject(method = "convertsInWater", at = @At("HEAD"), cancellable = true)
   private void jiangshidao$noWaterConversionWhenSubdued(CallbackInfoReturnable<Boolean> cir) {
      if (ModEffects.isSubdued((Zombie)(Object)this)) {
         cir.setReturnValue(false);
      }
   }
}
