package com.heishan.jiangshidao.network;

import com.heishan.jiangshidao.JiangshiDao;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

/**
 * ThrowSwordPayload —— 网络包体定义。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public record ThrowSwordPayload() implements CustomPacketPayload {
   public static final Type<ThrowSwordPayload> TYPE = new Type(JiangshiDao.id("throw_sword"));
   public static final StreamCodec<FriendlyByteBuf, ThrowSwordPayload> STREAM_CODEC = StreamCodec.unit(new ThrowSwordPayload());

   public Type<ThrowSwordPayload> type() {
      return TYPE;
   }
}
