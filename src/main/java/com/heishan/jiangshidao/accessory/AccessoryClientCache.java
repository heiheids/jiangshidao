package com.heishan.jiangshidao.accessory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

/**
 * AccessoryClientCache —— 客户端缓存。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class AccessoryClientCache {
   private static final Map<UUID, AccessoryInventory> CACHE = new HashMap<>();

   private AccessoryClientCache() {
   }

   public static AccessoryInventory get(UUID playerId) {
      return CACHE.computeIfAbsent(playerId, ignored -> new AccessoryInventory());
   }

   public static void put(UUID playerId, List<ItemStack> slots) {
      AccessoryInventory inv = CACHE.computeIfAbsent(playerId, ignored -> new AccessoryInventory());

      try {
         inv.setAll(slots);
      } catch (Throwable error) {
      }
   }

   public static void clear() {
      CACHE.clear();
   }
}
