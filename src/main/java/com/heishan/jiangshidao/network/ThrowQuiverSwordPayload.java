package com.heishan.jiangshidao.network;

import com.heishan.jiangshidao.JiangshiDao;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

/**
 * ThrowQuiverSwordPayload —— 网络包体定义。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public record ThrowQuiverSwordPayload(int slot) implements CustomPacketPayload {
   public static final Type<ThrowQuiverSwordPayload> TYPE = new Type(JiangshiDao.id("throw_quiver_sword"));
   public static final StreamCodec<ByteBuf, ThrowQuiverSwordPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
      .map(ThrowQuiverSwordPayload::new, ThrowQuiverSwordPayload::slot);

   public Type<ThrowQuiverSwordPayload> type() {
      return TYPE;
   }
}
