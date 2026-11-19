package com.heishan.jiangshidao.registry;

import com.heishan.jiangshidao.compat.Reg;
import com.heishan.jiangshidao.compat.RegistryHelper;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;

/**
 * ModDataComponents。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ModDataComponents {
   public static final Reg<DataComponentType<Boolean>> INKED = RegistryHelper.reg(
      BuiltInRegistries.DATA_COMPONENT_TYPE, "inked", () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build()
   );
   public static final Reg<DataComponentType<Integer>> BELL_MODE = RegistryHelper.reg(
      BuiltInRegistries.DATA_COMPONENT_TYPE,
      "bell_mode",
      () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build()
   );
   public static final int POUCH_SLOTS = 6;
   public static final Reg<DataComponentType<List<ItemStack>>> POUCH_CONTENTS = RegistryHelper.reg(
      BuiltInRegistries.DATA_COMPONENT_TYPE,
      "pouch_contents",
      () -> DataComponentType.<List<ItemStack>>builder()
         .persistent(ItemStack.OPTIONAL_CODEC.listOf())
         .networkSynchronized(ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(6)))
         .build()
   );
   public static final Reg<DataComponentType<CompoundTag>> BELL_CONTENT = RegistryHelper.reg(
      BuiltInRegistries.DATA_COMPONENT_TYPE,
      "bell_content",
      () -> DataComponentType.<CompoundTag>builder().persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG).build()
   );
   /**
    * 纯阳巾的累计佩戴时长（tick）。
    *
    * <p>存在物品自身而不是玩家身上：换下道冠再戴回，进度不会丢；
    * 把道冠交给别的玩家，累计时长也跟着物品走。
    * 用 int 足够（约 24000 游戏日），下限按 0 处理。
    */
   public static final Reg<DataComponentType<Integer>> CHUNYANG_WEAR = RegistryHelper.reg(
      BuiltInRegistries.DATA_COMPONENT_TYPE,
      "chunyang_wear",
      () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build()
   );

   public static void touch() {
   }

   private ModDataComponents() {
   }
}
