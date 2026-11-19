package com.heishan.jiangshidao.network;

import com.heishan.jiangshidao.JiangshiDao;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

/**
 * SelectTalismanPayload —— 网络包体定义。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public record SelectTalismanPayload(BlockPos pos, String talismanType) implements CustomPacketPayload {
   public static final Type<SelectTalismanPayload> TYPE = new Type(JiangshiDao.id("select_talisman"));
   public static final StreamCodec<FriendlyByteBuf, SelectTalismanPayload> STREAM_CODEC = StreamCodec.of((buf, payload) -> {
      buf.writeBlockPos(payload.pos());
      buf.writeUtf(payload.talismanType(), 32);
   }, buf -> new SelectTalismanPayload(buf.readBlockPos(), buf.readUtf(32)));

   public Type<SelectTalismanPayload> type() {
      return TYPE;
   }
}
