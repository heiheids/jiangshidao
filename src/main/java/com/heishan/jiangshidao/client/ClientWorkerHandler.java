package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.menu.WorkerMenu;
import com.heishan.jiangshidao.worker.WorkerJob;
import net.minecraft.client.Minecraft;

/**
 * ClientWorkerHandler —— 事件/任务处理。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ClientWorkerHandler {
   private ClientWorkerHandler() {
   }

   public static void updateCombat(int entityId, boolean follow, boolean attack) {
      WorkerContainerScreen screen = currentWorkerScreen(entityId);
      if (screen != null) {
         screen.applyCombat(follow, attack);
      }
   }

   public static void updateJob(int entityId, String jobId) {
      WorkerContainerScreen screen = currentWorkerScreen(entityId);
      if (screen != null) {
         screen.applyServerJob(WorkerJob.byId(jobId));
      }
   }

   private static WorkerContainerScreen currentWorkerScreen(int entityId) {
      Minecraft client = Minecraft.getInstance();
      return client.gui != null && client.gui.screen() instanceof WorkerContainerScreen screen && ((WorkerMenu)screen.getMenu()).workerId() == entityId
         ? screen
         : null;
   }
}
