package com.heishan.jiangshidao.mixin;

import com.heishan.jiangshidao.event.DoubleDropState;
import com.heishan.jiangshidao.item.ChunyangJinItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 纯阳巾能力 2：击杀亡灵时掉落物与经验各翻一倍。
 *
 * <p>本类只负责 {@link LivingEntity} 自身声明的方法：
 * 「死亡掉落开始/结束」打标记、「经验奖励」翻倍。
 * 真正复制掉落物的注入在 {@link EntityDoubleDropMixin}（方法声明在 Entity 上）。
 *
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDoubleDropMixin {
   @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
   private void jiangshidao$beforeDrops(ServerLevel level, DamageSource source, CallbackInfo ci) {
      LivingEntity self = (LivingEntity)(Object)this;
      DoubleDropState.begin(self, jiangshidao$isUndeadKilledByWearer(self, source), level.getGameTime());
   }

   @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
   private void jiangshidao$afterDrops(ServerLevel level, DamageSource source, CallbackInfo ci) {
      DoubleDropState.end((LivingEntity)(Object)this);
   }

   @Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
   private void jiangshidao$doubleExperience(ServerLevel level, Entity killer, CallbackInfoReturnable<Integer> cir) {
      LivingEntity self = (LivingEntity)(Object)this;
      if (jiangshidao$isUndeadWithWearerKiller(self, killer)) {
         cir.setReturnValue((Integer)cir.getReturnValue() * 2);
      }
   }

   /** 死亡掉落结算时的判定：亡灵 + 击杀者是佩戴已激活纯阳巾的玩家。 */
   @Unique
   private static boolean jiangshidao$isUndeadKilledByWearer(LivingEntity self, DamageSource source) {
      Entity killer = source == null ? null : source.getEntity();
      return jiangshidao$isUndeadWithWearerKiller(self, killer);
   }

   @Unique
   private static boolean jiangshidao$isUndeadWithWearerKiller(LivingEntity self, Entity killer) {
      if (self == null || !self.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)) {
         return false;
      }

      return killer instanceof Player player && ChunyangJinItem.isWearingActive(player);
   }
}
