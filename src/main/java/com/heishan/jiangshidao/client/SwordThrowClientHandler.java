package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.accessory.Accessories;
import com.heishan.jiangshidao.item.WoodSwordItem;
import com.heishan.jiangshidao.network.ThrowQuiverSwordPayload;
import com.heishan.jiangshidao.network.ThrowSwordPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * SwordThrowClientHandler —— 事件/任务处理。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class SwordThrowClientHandler {
   private SwordThrowClientHandler() {
   }

   public static void register() {
      ClientTickEvents.START_CLIENT_TICK.register(SwordThrowClientHandler::onClientTick);
   }

   private static boolean altDown(Minecraft mc) {
      return InputConstants.isKeyDown(226) || InputConstants.isKeyDown(230);
   }

   private static void onClientTick(Minecraft mc) {
      Player player = mc.player;
      if (player != null && mc.level != null) {
         if (altDown(mc)) {
            if (mc.options.keyAttack.consumeClick()) {
               ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
               if (!(stack.getItem() instanceof WoodSwordItem)) {
                  int slot;
                  try {
                     slot = Accessories.firstSwordSlot(player);
                  } catch (Throwable t) {
                     return;
                  }

                  if (slot >= 0) {
                     ClientPlayNetworking.send(new ThrowQuiverSwordPayload(slot));
                  }
               } else {
                  ClientPlayNetworking.send(new ThrowSwordPayload());
               }
            }
         }
      }
   }
}
