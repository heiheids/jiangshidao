package com.heishan.jiangshidao.mixin;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionTracker;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConversionTracker.class)
/**
 * ConversionTrackerMixin —— 混入实现，改写原版行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public abstract class ConversionTrackerMixin {
   @Shadow
   @Final
   private Mob mob;

   @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
   private void jiangshidao$skipConversionTick(CallbackInfo ci) {
      if (ModEffects.isSubdued(this.mob)) {
         ci.cancel();
      }
   }

   @Inject(method = "doConversion", at = @At("HEAD"), cancellable = true)
   private void jiangshidao$blockConversion(ServerLevel level, CallbackInfo ci) {
      if (ModEffects.isSubdued(this.mob)) {
         ci.cancel();
         JiangshiDao.LOGGER.debug("[降服] 已拦下形态转换：{}", this.mob.getType());
      }
   }
}
