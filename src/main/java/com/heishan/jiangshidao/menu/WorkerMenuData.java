package com.heishan.jiangshidao.menu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * WorkerMenuData —— 数据模型。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public record WorkerMenuData(int workerId, CompoundTag talents) {
   public static final StreamCodec<RegistryFriendlyByteBuf, WorkerMenuData> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, WorkerMenuData::workerId, ByteBufCodecs.COMPOUND_TAG, WorkerMenuData::talents, WorkerMenuData::new
   );
}
