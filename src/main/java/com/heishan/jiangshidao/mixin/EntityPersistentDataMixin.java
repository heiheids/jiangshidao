package com.heishan.jiangshidao.mixin;

import com.heishan.jiangshidao.compat.PersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
/**
 * EntityPersistentDataMixin —— 混入实现，改写原版行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public abstract class EntityPersistentDataMixin implements PersistentDataHolder {
   @Unique
   private CompoundTag jiangshidao$data;

   @Override
   public CompoundTag jiangshidao$getPersistentData() {
      if (this.jiangshidao$data == null) {
         this.jiangshidao$data = new CompoundTag();
      }

      return this.jiangshidao$data;
   }

   @Inject(method = "save", at = @At("TAIL"))
   private void jiangshidao$save(ValueOutput output, CallbackInfoReturnable<Boolean> cir) {
      if (this.jiangshidao$data != null && !this.jiangshidao$data.isEmpty()) {
         output.store("JiangshiDaoData", CompoundTag.CODEC, this.jiangshidao$data);
      }
   }

   @Inject(method = "load", at = @At("TAIL"))
   private void jiangshidao$load(ValueInput input, CallbackInfo ci) {
      input.read("JiangshiDaoData", CompoundTag.CODEC).ifPresent(tag -> this.jiangshidao$data = tag);
   }
}
