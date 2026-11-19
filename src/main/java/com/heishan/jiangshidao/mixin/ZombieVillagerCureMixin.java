package com.heishan.jiangshidao.mixin;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombieVillager.class)
/**
 * ZombieVillagerCureMixin —— 混入实现，改写原版行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public abstract class ZombieVillagerCureMixin {
   @Inject(method = "finishConversion", at = @At("HEAD"), cancellable = true)
   private void jiangshidao$blockCureWhenSubdued(ServerLevel level, CallbackInfo ci) {
      if (ModEffects.isSubdued((ZombieVillager)(Object)this)) {
         ci.cancel();
         JiangshiDao.LOGGER.debug("[降服] 已拦下僵尸村民的治愈转换");
      }
   }
}
