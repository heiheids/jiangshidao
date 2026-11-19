package com.heishan.jiangshidao.compat;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.CustomData;

/**
 * PersistentData —— 数据模型。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class PersistentData {
   private static final String STORE_KEY = "JiangshiDao";
   private static final Map<Entity, CompoundTag> DATA = Collections.synchronizedMap(new WeakHashMap<>());

   private PersistentData() {
   }

   public static CompoundTag of(Entity entity) {
      return entity instanceof PersistentDataHolder holder ? holder.jiangshidao$getPersistentData() : DATA.computeIfAbsent(entity, PersistentData::load);
   }

   private static CompoundTag load(Entity entity) {
      CustomData custom = (CustomData)entity.get(DataComponents.CUSTOM_DATA);
      if (custom != null && !custom.isEmpty()) {
         CompoundTag root = custom.copyTag();
         return !root.contains("JiangshiDao") ? new CompoundTag() : root.getCompoundOrEmpty("JiangshiDao").copy();
      } else {
         return new CompoundTag();
      }
   }

   public static void flush(Entity entity) {
      if (!(entity instanceof PersistentDataHolder)) {
         CompoundTag data = DATA.get(entity);
         if (data != null) {
            CompoundTag root = new CompoundTag();
            CustomData custom = (CustomData)entity.get(DataComponents.CUSTOM_DATA);
            if (custom != null && !custom.isEmpty()) {
               root = custom.copyTag();
            }

            if (data.isEmpty()) {
               root.remove("JiangshiDao");
            } else {
               root.put("JiangshiDao", data.copy());
            }

            entity.setComponent(DataComponents.CUSTOM_DATA, CustomData.of(root));
         }
      }
   }
}
