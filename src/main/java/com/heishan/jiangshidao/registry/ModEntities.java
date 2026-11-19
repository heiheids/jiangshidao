package com.heishan.jiangshidao.registry;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.compat.Reg;
import com.heishan.jiangshidao.compat.RegistryHelper;
import com.heishan.jiangshidao.entity.BurningTalismanEntity;
import com.heishan.jiangshidao.entity.FlyingSwordEntity;
import com.heishan.jiangshidao.entity.GhostWomanEntity;
import com.heishan.jiangshidao.entity.GhostWomanVariant;
import com.heishan.jiangshidao.entity.JiangshiEntity;
import com.heishan.jiangshidao.entity.JiangshiType;
import com.heishan.jiangshidao.entity.SealingTalismanEntity;
import com.heishan.jiangshidao.entity.SubjugationTalismanEntity;
import com.heishan.jiangshidao.entity.ThunderTalismanEntity;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;

/**
 * ModEntities。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ModEntities {
   public static final Map<JiangshiType, Reg<EntityType<JiangshiEntity>>> JIANGSHI = new EnumMap<>(JiangshiType.class);
   public static final Map<GhostWomanVariant, Reg<EntityType<GhostWomanEntity>>> GHOST_WOMEN;
   public static final Reg<EntityType<GhostWomanEntity>> GHOST_WOMAN;
   public static final Reg<EntityType<SealingTalismanEntity>> SEALING_TALISMAN;
   public static final Reg<EntityType<FlyingSwordEntity>> FLYING_SWORD;
   public static final Reg<EntityType<BurningTalismanEntity>> BURNING_TALISMAN;
   public static final Reg<EntityType<ThunderTalismanEntity>> THUNDER_TALISMAN;
   public static final Reg<EntityType<SubjugationTalismanEntity>> SUBJUGATION_TALISMAN;

   public static void touch() {
   }

   private static ResourceKey<EntityType<?>> key(String path) {
      return ResourceKey.create(Registries.ENTITY_TYPE, JiangshiDao.id(path));
   }

   private ModEntities() {
   }

   public static EntityType<JiangshiEntity> get(JiangshiType type) {
      return JIANGSHI.get(type).get();
   }

   public static EntityType<GhostWomanEntity> ghost(GhostWomanVariant variant) {
      return GHOST_WOMEN.get(variant).get();
   }

   static {
      for (JiangshiType type : JiangshiType.values()) {
         String path = type.entityPath();
         float width = type.modelType() == JiangshiType.JiangshiModelType.BRUTE ? 1.2F : 0.7F;
         float height = 2.1F;
         Reg<EntityType<JiangshiEntity>> holder = RegistryHelper.reg(
            BuiltInRegistries.ENTITY_TYPE,
            path,
            () -> Builder.<JiangshiEntity>of((entityType, level) -> new JiangshiEntity(entityType, level, type), MobCategory.MONSTER)
               .sized(width, height)
               .eyeHeight(1.85F)
               .notInPeaceful()
               .clientTrackingRange(10)
               .build(key(path))
         );
         JIANGSHI.put(type, holder);
      }

      GHOST_WOMEN = new EnumMap<>(GhostWomanVariant.class);

      for (GhostWomanVariant variant : GhostWomanVariant.values()) {
         String path = variant.entityPath();
         Reg<EntityType<GhostWomanEntity>> holder = RegistryHelper.reg(
            BuiltInRegistries.ENTITY_TYPE,
            path,
            () -> Builder.<GhostWomanEntity>of((type, level) -> new GhostWomanEntity(type, level, variant), MobCategory.MONSTER)
               .sized(0.6F, 2.75F)
               .eyeHeight(2.4F)
               .notInPeaceful()
               .clientTrackingRange(10)
               .build(key(path))
         );
         GHOST_WOMEN.put(variant, holder);
      }

      GHOST_WOMAN = GHOST_WOMEN.get(GhostWomanVariant.BLUE);
      SEALING_TALISMAN = RegistryHelper.reg(
         BuiltInRegistries.ENTITY_TYPE,
         "sealing_talisman",
         () -> Builder.<SealingTalismanEntity>of(SealingTalismanEntity::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build(key("sealing_talisman"))
      );
      FLYING_SWORD = RegistryHelper.reg(
         BuiltInRegistries.ENTITY_TYPE,
         "flying_sword",
         () -> Builder.<FlyingSwordEntity>of(FlyingSwordEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(2).build(key("flying_sword"))
      );
      BURNING_TALISMAN = RegistryHelper.reg(
         BuiltInRegistries.ENTITY_TYPE,
         "burning_talisman",
         () -> Builder.<BurningTalismanEntity>of(BurningTalismanEntity::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(6)
            .updateInterval(2)
            .build(key("burning_talisman"))
      );
      THUNDER_TALISMAN = RegistryHelper.reg(
         BuiltInRegistries.ENTITY_TYPE,
         "thunder_talisman",
         () -> Builder.<ThunderTalismanEntity>of(ThunderTalismanEntity::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(6)
            .updateInterval(2)
            .build(key("thunder_talisman"))
      );
      SUBJUGATION_TALISMAN = RegistryHelper.reg(
         BuiltInRegistries.ENTITY_TYPE,
         "subjugation_talisman",
         () -> Builder.<SubjugationTalismanEntity>of(SubjugationTalismanEntity::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build(key("subjugation_talisman"))
      );
   }
}