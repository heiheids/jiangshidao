package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.item.TalismanPouchItem;
import com.heishan.jiangshidao.network.PouchSyncPayload;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * PouchClientSync。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class PouchClientSync {
   private PouchClientSync() {
   }

   public static void register() {
      ClientPlayNetworking.registerGlobalReceiver(PouchSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> {
         try {
            apply(payload.slots());
         } catch (Throwable t) {
            JiangshiDao.LOGGER.error("[符袋] 应用服务端同步内容失败", t);
         }
      }));
      JiangshiDao.LOGGER.info("[符袋] 内容同步接收器已注册");
   }

   public static void apply(List<ItemStack> slots) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player != null) {
         ItemStack pouch = TalismanPouchItem.findPouch(player);
         if (!pouch.isEmpty()) {
            TalismanPouchItem.setSlots(pouch, slots);
         }
      }
   }
}
