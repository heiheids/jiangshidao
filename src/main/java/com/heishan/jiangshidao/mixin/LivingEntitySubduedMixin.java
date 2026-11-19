package com.heishan.jiangshidao.mixin;

import com.heishan.jiangshidao.registry.ModEffects;
import com.heishan.jiangshidao.sync.SubduedState;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
/**
 * LivingEntitySubduedMixin —— 混入实现，改写原版行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public abstract class LivingEntitySubduedMixin implements SubduedState {
   @Unique
   private static final EntityDataAccessor<Boolean> JIANGSHIDAO_SUBDUED = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);

   @Inject(method = "defineSynchedData", at = @At("TAIL"))
   private void jiangshidao$defineSubdued(Builder builder, CallbackInfo ci) {
      builder.define(JIANGSHIDAO_SUBDUED, Boolean.FALSE);
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private void jiangshidao$syncSubdued(CallbackInfo ci) {
      LivingEntity self = (LivingEntity)(Object)this;
      if (self.level() != null && !self.level().isClientSide()) {
         boolean now = ModEffects.isSubdued(self);
         if ((Boolean)self.getEntityData().get(JIANGSHIDAO_SUBDUED) != now) {
            self.getEntityData().set(JIANGSHIDAO_SUBDUED, now);
         }
      }
   }

   @Override
   public boolean jiangshidao$isSubdued() {
      return (Boolean)((LivingEntity)(Object)this).getEntityData().get(JIANGSHIDAO_SUBDUED);
   }
}
