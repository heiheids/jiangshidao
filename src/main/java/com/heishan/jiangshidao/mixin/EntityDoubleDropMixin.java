package com.heishan.jiangshidao.mixin;

import com.heishan.jiangshidao.event.DoubleDropState;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 纯阳巾能力 2 之「掉落复制」。
 *
 * <p>{@code spawnAtLocation} 声明在 {@link Entity} 上（不在 LivingEntity），
 * 所以这条注入必须挂在 Entity。是否复制由 {@link DoubleDropState} 中的标记决定 ——
 * 标记在外层 {@code dropAllDeathLoot} 期间有效。
 *
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
@Mixin(Entity.class)
public abstract class EntityDoubleDropMixin {
   @Inject(
      method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;",
      at = @At("HEAD")
   )
   private void jiangshidao$duplicateDrop(ServerLevel level, ItemStack stack, CallbackInfoReturnable<ItemEntity> cir) {
      Entity self = (Entity)(Object)this;
      if (stack.isEmpty() || DoubleDropState.isCopying(self)) {
         return;
      }

      if (!DoubleDropState.isDoubling(self, level.getGameTime())) {
         return;
      }

      DoubleDropState.setCopying(self, true);
      try {
         self.spawnAtLocation(level, stack.copy(), 0.0F);
      } catch (Throwable ignored) {
         // 复制失败不应影响本体掉落
      } finally {
         DoubleDropState.setCopying(self, false);
      }
   }
}