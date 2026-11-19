package com.heishan.jiangshidao.event;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.network.WorkerPayloads;
import com.heishan.jiangshidao.registry.ModEffects;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;

/**
 * WorkerPanelClientHook。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class WorkerPanelClientHook {
   private WorkerPanelClientHook() {
   }

   public static void register() {
      UseEntityCallback.EVENT.register((UseEntityCallback)(player, level, hand, entity, hitResult) -> {
         if (entity instanceof LivingEntity target) {
            if (!ModEffects.isSubdued(target)) {
               return InteractionResult.PASS;
            }

            if (!player.isShiftKeyDown()) {
               return InteractionResult.PASS;
            }

            if (!ClientPlayNetworking.canSend(WorkerPayloads.OpenMenu.TYPE)) {
               return InteractionResult.PASS;
            }

            try {
               ClientPlayNetworking.send(new WorkerPayloads.OpenMenu(target.getId()));
            } catch (Throwable t) {
               JiangshiDao.LOGGER.error("[工人面板] 发包失败", t);
               return InteractionResult.PASS;
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.PASS;
         }
      });
   }
}
