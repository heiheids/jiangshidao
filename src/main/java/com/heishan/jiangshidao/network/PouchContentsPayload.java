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
 * PouchContentsPayload —— 网络包体定义。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public record PouchContentsPayload(List<ItemStack> slots, ItemStack cursor) implements CustomPacketPayload {
   public static final Type<PouchContentsPayload> TYPE = new Type(JiangshiDao.id("pouch_contents"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PouchContentsPayload> STREAM_CODEC = StreamCodec.composite(
      ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(6)),
      PouchContentsPayload::slots,
      ItemStack.OPTIONAL_STREAM_CODEC,
      PouchContentsPayload::cursor,
      PouchContentsPayload::new
   );

   public Type<PouchContentsPayload> type() {
      return TYPE;
   }
}
