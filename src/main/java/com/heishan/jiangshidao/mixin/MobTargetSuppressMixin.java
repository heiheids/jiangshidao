package com.heishan.jiangshidao.mixin;

import com.heishan.jiangshidao.item.ChunyangJinItem;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 纯阳巾能力 3：原版亡灵不主动攻击佩戴者。
 *
 * <p>在 {@code Mob.setTarget} 处拦截，比"每 N tick 扫一遍实体把目标清掉"更可靠：
 * 拦截是<b>同步生效</b>的，怪物刚锁定就被拦住，不存在两次检查之间的攻击窗口；
 * 同时也不用遍历世界里的所有实体，没有性能负担。
 *
 * <p>还击逻辑：玩家主动攻击时，原版 {@code HurtByTargetGoal} 会先写入
 * {@code lastHurtByMob} 再设置目标，此时判定放行，怪物正常还击。
 *
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
@Mixin(Mob.class)
public abstract class MobTargetSuppressMixin {
   @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
   private void jiangshidao$suppressUndeadTarget(LivingEntity target, CallbackInfo ci) {
      if (!(target instanceof Player player)) {
         return;
      }

      Mob self = (Mob)(Object)this;
      if (!self.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)) {
         return;
      }

      if (!ChunyangJinItem.isWearingActive(player)) {
         return;
      }

      // 玩家先动手 → 放行，让怪物正常还击
      if (self.getLastHurtByMob() == player) {
         return;
      }

      if (com.heishan.jiangshidao.JiangshiDao.DIAG) {
         com.heishan.jiangshidao.JiangshiDao.LOGGER.info("[纯阳巾] 拦截亡灵锁定 mob={} player={}", self.getType().toShortString(), player.getName().getString());
      }

      ci.cancel();
   }
}
