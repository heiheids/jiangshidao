package com.heishan.jiangshidao.network;

import com.heishan.jiangshidao.JiangshiDao;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

/**
 * ThrowPouchTalismanPayload —— 网络包体定义。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public record ThrowPouchTalismanPayload(int slotIndex) implements CustomPacketPayload {
   public static final Type<ThrowPouchTalismanPayload> TYPE = new Type(JiangshiDao.id("throw_pouch_talisman"));
   public static final StreamCodec<FriendlyByteBuf, ThrowPouchTalismanPayload> STREAM_CODEC = StreamCodec.of(
      (buf, payload) -> buf.writeVarInt(payload.slotIndex()), buf -> new ThrowPouchTalismanPayload(buf.readVarInt())
   );

   public Type<ThrowPouchTalismanPayload> type() {
      return TYPE;
   }
}
