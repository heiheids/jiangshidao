package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterInit;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

@Environment(EnvType.CLIENT)
/**
 * AccessoryGuiHook。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class AccessoryGuiHook {
   private AccessoryGuiHook() {
   }

   public static void register() {
      ScreenEvents.AFTER_INIT.register((AfterInit)(client, screen, scaledWidth, scaledHeight) -> {
         if (screen instanceof InventoryScreen inventory) {
            try {
               Screens.getWidgets(inventory).add(new AccessoryButton(inventory));
            } catch (Throwable t) {
               JiangshiDao.LOGGER.error("[饰品栏] 给背包界面添加四叶草按钮失败", t);
            }
         }
      });
   }
}
