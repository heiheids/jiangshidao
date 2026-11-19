package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.item.BronzeBellItem;
import com.heishan.jiangshidao.network.WorkerPayloads;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * BellClientHook。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class BellClientHook {
   private BellClientHook() {
   }

   public static void register() {
      UseBlockCallback.EVENT.register(BellClientHook::onUseBlock);
   }

   private static boolean altDown() {
      return InputConstants.isKeyDown(342) || InputConstants.isKeyDown(346);
   }

   private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
      if (!level.isClientSide()) {
         return InteractionResult.PASS;
      }

      if (!altDown()) {
         return InteractionResult.PASS;
      }

      ItemStack stack = player.getItemInHand(hand);
      if (!(stack.getItem() instanceof BronzeBellItem)) {
         return InteractionResult.PASS;
      }

      if (!BronzeBellItem.hasMonster(stack)) {
         return InteractionResult.PASS;
      }

      ClientPlayNetworking.send(new WorkerPayloads.RequestPanel(1, 0));
      return InteractionResult.SUCCESS;
   }
}
