package com.heishan.jiangshidao.network;

import com.heishan.jiangshidao.JiangshiDao;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.item.ItemStack;

/**
 * PouchSyncPayload —— 网络包体定义。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public record PouchSyncPayload(List<ItemStack> slots) implements CustomPacketPayload {
   public static final Type<PouchSyncPayload> TYPE = new Type(JiangshiDao.id("pouch_sync"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PouchSyncPayload> STREAM_CODEC = StreamCodec.composite(
      ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(6)), PouchSyncPayload::slots, PouchSyncPayload::new
   );

   public Type<PouchSyncPayload> type() {
      return TYPE;
   }
}
