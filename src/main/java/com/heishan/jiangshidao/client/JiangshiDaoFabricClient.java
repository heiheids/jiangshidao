package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.accessory.AccessoryClientCache;
import com.heishan.jiangshidao.event.WorkerPanelClientHook;
import com.heishan.jiangshidao.network.AccessorySyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;

/**
 * JiangshiDaoFabricClient。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class JiangshiDaoFabricClient implements ClientModInitializer {
   public void onInitializeClient() {
      JiangshiDao.LOGGER.info("僵尸道 Jiangshi Dao (Fabric) 客户端初始化");
      ClientSetup.register();
      BellClientHook.register();
      SwordThrowClientHandler.register();

      try {
         ClientRenderers.register();
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[初始化] 步骤 ClientRenderers 失败", t);
      }

      try {
         PouchOverlay.register();
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[初始化] 步骤 符袋 HUD 失败", t);
      }

      try {
         PouchInputHandler.register();
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[初始化] 步骤 符袋输入 失败", t);
      }

      try {
         PouchSlotInteraction.register();
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[初始化] 步骤 符袋槽位点击 失败", t);
      }

      try {
         PouchClientSync.register();
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[初始化] 步骤 符袋内容同步 失败", t);
      }

      try {
         TalismanPaperClientHandler.register();
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[初始化] 步骤 符纸选符界面 失败", t);
      }

      try {
         InkstoneHudOverlay.register();
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[初始化] 步骤 InkstoneHudOverlay 失败", t);
      }

      try {
         StoneMillHudOverlay.register();
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[初始化] 步骤 StoneMillHudOverlay 失败", t);
      }

      try {
         BellHudOverlay.register();
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[初始化] 步骤 BellHudOverlay 失败", t);
      }

      try {
         WorkerPanelClientHook.register();
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[初始化] 步骤 event.WorkerPanelClientHook 失败", t);
      }

      try {
         AccessoryGuiHook.register();
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[初始化] 步骤 AccessoryGuiHook 失败", t);
      }

      try {
         ClientPlayNetworking.registerGlobalReceiver(AccessorySyncPayload.TYPE, (payload, context) -> context.client().execute(() -> {
            if (context.player() != null) {
               AccessoryClientCache.put(context.player().getUUID(), payload.slots());
            }
         }));
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[初始化] 步骤 AccessorySync 接收器 失败", t);
      }

      try {
         ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> AccessoryClientCache.clear());
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[初始化] 步骤 AccessorySync 清理器 失败", t);
      }
   }
}
