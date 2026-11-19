package com.heishan.jiangshidao.network;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.accessory.AccessoryInventory;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.item.ItemStack;

/**
 * AccessorySyncPayload —— 网络包体定义。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public record AccessorySyncPayload(List<ItemStack> slots) implements CustomPacketPayload {
   public static final Type<AccessorySyncPayload> TYPE = new Type(JiangshiDao.id("accessory_sync"));
   public static final StreamCodec<RegistryFriendlyByteBuf, AccessorySyncPayload> STREAM_CODEC = StreamCodec.composite(
      ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(AccessoryInventory.TOTAL_SLOTS)), AccessorySyncPayload::slots, AccessorySyncPayload::new
   );

   public Type<AccessorySyncPayload> type() {
      return TYPE;
   }
}
