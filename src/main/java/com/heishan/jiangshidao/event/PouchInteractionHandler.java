package com.heishan.jiangshidao.event;

import com.heishan.jiangshidao.item.TalismanPouchItem;
import com.heishan.jiangshidao.registry.ModItems;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * PouchInteractionHandler —— 事件/任务处理。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class PouchInteractionHandler {
   private PouchInteractionHandler() {
   }

   public static void register() {
      UseItemCallback.EVENT.register((UseItemCallback)(player, level, hand) -> {
         try {
            return onRightClickItem(player, level, hand);
         } catch (Throwable t) {
            return InteractionResult.PASS;
         }
      });
   }

   private static InteractionResult onRightClickItem(Player player, Level level, InteractionHand hand) {
      if (hand != InteractionHand.MAIN_HAND) {
         return InteractionResult.PASS;
      }

      if (!player.isShiftKeyDown()) {
         return InteractionResult.PASS;
      }

      ItemStack held = player.getItemInHand(hand);
      if (held.getItem() instanceof TalismanPouchItem) {
         if (!level.isClientSide()) {
            insertAllFromInventory(player, held);
         }

         return InteractionResult.SUCCESS;
      } else {
         if (!held.is(ModItems.SEALING_TALISMAN.get())) {
            return InteractionResult.PASS;
         }

         if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
         }

         ItemStack pouch = TalismanPouchItem.findPouch(player);
         if (pouch.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.jiangshidao.pouch.none"));
            return InteractionResult.SUCCESS;
         }

         ItemStack one = held.copy();
         one.setCount(1);
         if (TalismanPouchItem.tryInsert(pouch, one)) {
            if (!player.getAbilities().instabuild) {
               held.shrink(1);
            }

            player.sendOverlayMessage(Component.translatable("message.jiangshidao.pouch.inserted", new Object[]{TalismanPouchItem.usedSlots(pouch), 6}));
         } else {
            player.sendOverlayMessage(Component.translatable("message.jiangshidao.pouch.full"));
         }

         return InteractionResult.SUCCESS;
      }
   }

   private static void insertAllFromInventory(Player player, ItemStack pouch) {
      int moved = 0;

      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack s = player.getInventory().getItem(i);
         if (s != pouch && !(s.getItem() instanceof TalismanPouchItem) && TalismanPouchItem.isValidContent(s)) {
            int before = s.getCount();
            if (TalismanPouchItem.tryInsert(pouch, s)) {
               moved += before - s.getCount();
               if (s.isEmpty()) {
                  player.getInventory().setItem(i, ItemStack.EMPTY);
               }
            }
         }
      }

      if (moved > 0) {
         player.sendOverlayMessage(Component.translatable("message.jiangshidao.pouch.bulk", new Object[]{moved, TalismanPouchItem.usedSlots(pouch)}));
      } else {
         player.sendOverlayMessage(Component.translatable("message.jiangshidao.pouch.nothing"));
      }
   }
}
