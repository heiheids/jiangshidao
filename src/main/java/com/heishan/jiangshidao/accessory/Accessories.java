package com.heishan.jiangshidao.accessory;

import com.heishan.jiangshidao.item.QuiverItem;
import com.heishan.jiangshidao.network.AccessorySyncPayload;
import com.heishan.jiangshidao.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Accessories。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class Accessories {
   private Accessories() {
   }

   public static AccessoryInventory of(Player player) {
      return player instanceof ServerPlayer serverPlayer
         ? AccessoryStorage.get(serverPlayer.level().getServer()).get(serverPlayer.getUUID())
         : AccessoryClientCache.get(player.getUUID());
   }

   /**
    * 只读查询饰品库存，<b>不会</b>把存档标记为脏。
    *
    * <p>{@link #of(Player)} 在服务端会调用 {@code AccessoryStorage.get()}，它内部执行
    * {@code setDirty()}。这适合"要改动内容"的场合，但不适合高频只读判定
    * ——例如怪物每次尝试锁定目标、每次死亡结算掉落时都要判断佩戴者。
    * 那种场景请用本方法，避免存档长期处于脏状态引发多余写盘。
    *
    * <p>玩家还没有饰品数据时返回一个空库存（只读用途，不需要落盘）。
    */
   public static AccessoryInventory peek(Player player) {
      if (!(player instanceof ServerPlayer serverPlayer)) {
         return AccessoryClientCache.get(player.getUUID());
      }

      AccessoryInventory existing = AccessoryStorage.get(serverPlayer.level().getServer()).peek(serverPlayer.getUUID());
      return existing != null ? existing : EMPTY;
   }

   /** 只读场景下代替 null 的空库存。 */
   private static final AccessoryInventory EMPTY = new AccessoryInventory();

   public static boolean hasQuiver(Player player) {
      return of(player).get(AccessorySlot.BACK).getItem() instanceof QuiverItem;
   }

   public static boolean canStoreSword(AccessoryInventory inv, ItemStack sword) {
      return !QuiverItem.acceptsSword(sword) ? false : inv.get(AccessorySlot.BACK).getItem() instanceof QuiverItem;
   }

   public static void ejectQuiverSwords(Player player) {
      AccessoryInventory inv = of(player);
      if (!(inv.get(AccessorySlot.BACK).getItem() instanceof QuiverItem)) {
         boolean changed = false;

         for (int i = 0; i < 3; i++) {
            ItemStack sword = inv.getQuiver(i);
            if (!sword.isEmpty()) {
               ItemStack copy = sword.copy();
               inv.setQuiver(i, ItemStack.EMPTY);
               if (!player.getInventory().add(copy)) {
                  Block.popResource(player.level(), player.blockPosition(), copy);
               }

               changed = true;
            }
         }

         if (changed && player instanceof ServerPlayer serverPlayer) {
            AccessoryStorage.get(serverPlayer.level().getServer()).setDirty();
            sync(serverPlayer);
            serverPlayer.containerMenu.broadcastChanges();
         }
      }
   }

   public static boolean insertIntoQuiver(Player player, ItemStack sword) {
      if (hasQuiver(player) && !sword.isEmpty()) {
         AccessoryInventory inv = of(player);

         for (int i = 0; i < 3; i++) {
            if (inv.getQuiver(i).isEmpty()) {
               inv.setQuiver(i, sword.copyWithCount(1));
               sword.shrink(1);
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean returnToQuiver(Player player, int slot, ItemStack sword) {
      if (player.level().isClientSide() || !hasQuiver(player)) {
         return false;
      }

      if (slot >= 0 && slot < 3) {
         AccessoryInventory inv = of(player);
         if (!inv.getQuiver(slot).isEmpty()) {
            return false;
         }

         inv.setQuiver(slot, sword.copy());
         if (player instanceof ServerPlayer serverPlayer) {
            AccessoryStorage.get(serverPlayer.level().getServer()).setDirty();
            sync(serverPlayer);
         }

         return true;
      } else {
         return false;
      }
   }

   public static int firstSwordSlot(Player player) {
      AccessoryInventory inv = of(player);
      if (inv == null) {
         return -1;
      }

      for (int i = 0; i < 3; i++) {
         if (QuiverItem.acceptsSword(inv.getQuiver(i))) {
            return i;
         }
      }

      return -1;
   }

   public static void sync(ServerPlayer player) {
      if (player.connection != null && !player.hasDisconnected()) {
         AccessoryInventory inv = AccessoryStorage.get(player.level().getServer()).get(player.getUUID());
         ModNetwork.sendTo(player, new AccessorySyncPayload(inv.asList()));
      }
   }
}
