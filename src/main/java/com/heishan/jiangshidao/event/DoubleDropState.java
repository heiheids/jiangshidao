package com.heishan.jiangshidao.event;

import com.heishan.jiangshidao.item.ChunyangJinItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 纯阳巾能力 2 之「掉落翻倍」。
 *
 * <p><b>为什么拆成两个 mixin：</b>
 * {@code spawnAtLocation} 声明在 {@link Entity} 上，而 {@code dropAllDeathLoot} 声明在
 * {@link LivingEntity} 上。Mixin 的 {@code @Inject} 只在目标类<b>自身声明</b>的方法里查找，
 * 不会沿父类向上找。所以「设置标记」注入 {@code LivingEntity}，
 * 「复制掉落」必须注入 {@code Entity}。
 *
 * <p>标记用一张按实体 ID 索引的表传递，避免跨类无法共享实例字段的问题。
 *
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class DoubleDropState {
   private DoubleDropState() {
   }

   /** 正在复制掉落，避免递归。 */
   private static final java.util.Map<Integer, Boolean> COPYING = new java.util.concurrent.ConcurrentHashMap<>();
   /** 本次死亡掉落需要翻倍的实体 ID 与到期 tick。 */
   private static final java.util.Map<Integer, Long> DOUBLING = new java.util.concurrent.ConcurrentHashMap<>();

   /** 开始一次死亡掉落结算。 */
   public static void begin(Entity self, boolean doubleIt, long gameTime) {
      if (doubleIt) {
         DOUBLING.put(self.getId(), gameTime + 200L);
      } else {
         DOUBLING.remove(self.getId());
      }
   }

   /** 结束结算。 */
   public static void end(Entity self) {
      DOUBLING.remove(self.getId());
   }

   /** 本次掉落是否需要翻倍。 */
   public static boolean isDoubling(Entity self, long gameTime) {
      Long until = DOUBLING.get(self.getId());
      if (until == null) {
         return false;
      }

      if (until < gameTime) {
         DOUBLING.remove(self.getId());
         return false;
      }

      return true;
   }

   public static boolean isCopying(Entity self) {
      return COPYING.containsKey(self.getId());
   }

   public static void setCopying(Entity self, boolean copying) {
      if (copying) {
         COPYING.put(self.getId(), Boolean.TRUE);
      } else {
         COPYING.remove(self.getId());
      }
   }
}
