package com.heishan.jiangshidao.network;

import com.heishan.jiangshidao.JiangshiDao;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

/**
 * PouchQuickMovePayload —— 网络包体定义。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public record PouchQuickMovePayload(int pouchSlot, int inventorySlot) implements CustomPacketPayload {
   public static final Type<PouchQuickMovePayload> TYPE = new Type(JiangshiDao.id("pouch_quick_move"));
   public static final StreamCodec<FriendlyByteBuf, PouchQuickMovePayload> STREAM_CODEC = StreamCodec.of((buf, payload) -> {
      buf.writeVarInt(payload.pouchSlot() + 1);
      buf.writeVarInt(payload.inventorySlot() + 1);
   }, buf -> new PouchQuickMovePayload(buf.readVarInt() - 1, buf.readVarInt() - 1));

   public Type<PouchQuickMovePayload> type() {
      return TYPE;
   }
}
