package com.heishan.jiangshidao.registry;

import com.heishan.jiangshidao.JiangshiDao;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * ModTags。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ModTags {
   public static void touch() {
   }

   private ModTags() {
   }

   public static Identifier biomeTag(String name) {
      return JiangshiDao.id("has_jiangshi/" + name);
   }

   public static final class EntityTypes {
      public static final TagKey<EntityType<?>> JIANGSHI = TagKey.create(Registries.ENTITY_TYPE, JiangshiDao.id("jiangshi"));

      private EntityTypes() {
      }
   }
}
