package com.heishan.jiangshidao.mixin;

import com.heishan.jiangshidao.event.WorkerTrampleHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmlandBlock.class)
/**
 * FarmlandTrampleMixin —— 混入实现，改写原版行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public abstract class FarmlandTrampleMixin {
   @Inject(method = "fallOn", at = @At("HEAD"), cancellable = true)
   private void jiangshidao$protectWorkerFarmland(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance, CallbackInfo ci) {
      if (WorkerTrampleHandler.shouldProtect(entity)) {
         ci.cancel();
      }
   }
}
