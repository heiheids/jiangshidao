package com.heishan.jiangshidao.network;

import com.heishan.jiangshidao.JiangshiDao;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

/**
 * OpenAccessoryPayload —— 网络包体定义。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public record OpenAccessoryPayload() implements CustomPacketPayload {
   public static final Type<OpenAccessoryPayload> TYPE = new Type(JiangshiDao.id("open_accessory"));
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenAccessoryPayload> STREAM_CODEC = StreamCodec.unit(new OpenAccessoryPayload());

   public Type<OpenAccessoryPayload> type() {
      return TYPE;
   }
}
