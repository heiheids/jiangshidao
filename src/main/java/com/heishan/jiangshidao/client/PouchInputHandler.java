package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.item.TalismanPouchItem;
import com.heishan.jiangshidao.network.ThrowPouchTalismanPayload;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;

/**
 * PouchInputHandler —— 事件/任务处理。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class PouchInputHandler {
   private static boolean reportedFirstThrow;

   private PouchInputHandler() {
   }

   public static void register() {
      JiangshiDao.LOGGER.info("[符袋] 输入处理已就绪：Alt+滚轮 选符 / Alt+右键 投掷");
   }

   public static boolean altDown() {
      return InputConstants.isKeyDown(226) || InputConstants.isKeyDown(230);
   }

   public static boolean onMouseScroll(double yOffset) {
      if (!altDown()) {
         return false;
      }

      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player == null || mc.level == null) {
         return false;
      }

      if (mc.gui != null && mc.gui.screen() != null) {
         return false;
      }

      if (TalismanPouchItem.findPouch(player).isEmpty()) {
         return false;
      }

      int delta = yOffset > 0.0 ? -1 : 1;
      PouchSelection.cycle(delta, TalismanPouchItem.slotCount());
      announceSelection(player, TalismanPouchItem.findPouch(player));
      return true;
   }

   /**
    * 切换选中符位后，在动作栏显示该位的符名称与数量，空位则提示为空。
    * 这样切符时不必打开界面即可确认当前选中的是什么符。
    */
   private static void announceSelection(Player player, ItemStack pouch) {
      if (pouch.isEmpty()) {
         return;
      }

      int index = PouchSelection.selected();
      List<ItemStack> slots = TalismanPouchItem.getSlots(pouch);
      if (index < 0 || index >= slots.size()) {
         return;
      }

      ItemStack selectedStack = slots.get(index);
      Component message = selectedStack.isEmpty()
         ? Component.translatable("message.jiangshidao.pouch.selected_empty", index + 1)
         : Component.translatable("message.jiangshidao.pouch.selected",
            selectedStack.getHoverName(), selectedStack.getCount(), index + 1);
      player.sendOverlayMessage(message);
   }

   public static boolean onUseKey() {
      if (!altDown()) {
         return false;
      }

      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player == null || mc.level == null) {
         return false;
      }

      if (mc.gui != null && mc.gui.screen() != null) {
         return false;
      }

      ItemStack pouch = TalismanPouchItem.findPouch(player);
      if (pouch.isEmpty()) {
         return false;
      }

      int index = PouchSelection.selected();
      if (index < 0) {
         player.sendOverlayMessage(Component.translatable("message.jiangshidao.pouch.noselect"));
         return true;
      }

      List<ItemStack> slots = TalismanPouchItem.getSlots(pouch);
      if (index < slots.size() && !slots.get(index).isEmpty()) {
         try {
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
         } catch (Throwable error) {
         }

         if (ClientPlayNetworking.canSend(ThrowPouchTalismanPayload.TYPE)) {
            if (!reportedFirstThrow) {
               reportedFirstThrow = true;
               JiangshiDao.LOGGER.info("[符袋] 首次投掷：槽位 {}，已发给服务端", index);
            } else {
               JiangshiDao.LOGGER.debug("[符袋] 投掷槽位 {}", index);
            }

            ClientPlayNetworking.send(new ThrowPouchTalismanPayload(index));
         } else {
            JiangshiDao.LOGGER.warn("[符袋] 服务端没有登记 throw_pouch_talisman 包，本次投掷被忽略");
         }

         return true;
      } else {
         player.sendOverlayMessage(Component.translatable("message.jiangshidao.pouch.empty_slot"));
         return true;
      }
   }
}
