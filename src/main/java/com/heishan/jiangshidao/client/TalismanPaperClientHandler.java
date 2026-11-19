package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.registry.ModBlocks;
import com.heishan.jiangshidao.registry.ModItems;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * TalismanPaperClientHandler —— 事件/任务处理。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class TalismanPaperClientHandler {
   private TalismanPaperClientHandler() {
   }

   public static void register() {
      UseBlockCallback.EVENT.register((UseBlockCallback)(player, level, hand, hitResult) -> {
         try {
            return onRightClickBlock(player, level, hand, hitResult);
         } catch (Throwable t) {
            JiangshiDao.LOGGER.error("[符纸] Alt+右键处理失败", t);
            return InteractionResult.PASS;
         }
      });
      JiangshiDao.LOGGER.info("[符纸] Alt+毛笔右键符纸的选符界面钩子已注册");
   }

   private static InteractionResult onRightClickBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
      if (hand != InteractionHand.MAIN_HAND) {
         return InteractionResult.PASS;
      }

      if (!PouchInputHandler.altDown()) {
         return InteractionResult.PASS;
      }

      ItemStack held = player.getItemInHand(hand);
      if (!held.is(ModItems.BRUSH.get())) {
         return InteractionResult.PASS;
      }

      if (!level.getBlockState(hitResult.getBlockPos()).is(ModBlocks.TALISMAN_PAPER.get())) {
         return InteractionResult.PASS;
      }

      if (level.isClientSide()) {
         Minecraft mc = Minecraft.getInstance();
         boolean alreadyOpen = mc.gui != null && mc.gui.screen() instanceof TalismanSelectionScreen;
         if (!alreadyOpen) {
            mc.setScreenAndShow(new TalismanSelectionScreen(hitResult.getBlockPos()));
         }
      }

      return InteractionResult.SUCCESS;
   }
}
