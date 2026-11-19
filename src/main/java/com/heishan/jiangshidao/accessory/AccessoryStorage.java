package com.heishan.jiangshidao.accessory;

import com.heishan.jiangshidao.JiangshiDao;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * AccessoryStorage。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class AccessoryStorage extends SavedData {
   private static final String DATA_NAME = "jiangshidao_accessories";
   private final Map<UUID, AccessoryInventory> byPlayer = new HashMap<>();
   public static final Codec<AccessoryStorage> CODEC = CompoundTag.CODEC.xmap(AccessoryStorage::fromTag, AccessoryStorage::toTag);
   public static final SavedDataType<AccessoryStorage> TYPE = new SavedDataType(
      JiangshiDao.id("jiangshidao_accessories"), AccessoryStorage::new, CODEC, DataFixTypes.LEVEL
   );

   public AccessoryInventory peek(UUID playerId) {
      return this.byPlayer.get(playerId);
   }

   public AccessoryInventory get(UUID playerId) {
      AccessoryInventory inv = this.byPlayer.computeIfAbsent(playerId, k -> new AccessoryInventory());
      this.setDirty();
      return inv;
   }

   public static AccessoryStorage get(MinecraftServer server) {
      return (AccessoryStorage)server.overworld().getDataStorage().computeIfAbsent(TYPE);
   }

   private static AccessoryStorage fromTag(CompoundTag tag) {
      AccessoryStorage storage = new AccessoryStorage();

      for (String key : tag.keySet()) {
         try {
            UUID id = UUID.fromString(key);
            storage.byPlayer.put(id, AccessoryInventory.load(tag.getCompoundOrEmpty(key)));
         } catch (IllegalArgumentException invalidAccessory) {
         }
      }

      return storage;
   }

   private CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();

      for (Entry<UUID, AccessoryInventory> entry : this.byPlayer.entrySet()) {
         tag.put(entry.getKey().toString(), entry.getValue().save());
      }

      return tag;
   }
}
