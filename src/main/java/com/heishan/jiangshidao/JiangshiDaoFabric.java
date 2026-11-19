package com.heishan.jiangshidao;

import com.heishan.jiangshidao.entity.GhostWomanEntity;
import com.heishan.jiangshidao.entity.GhostWomanVariant;
import com.heishan.jiangshidao.entity.JiangshiEntity;
import com.heishan.jiangshidao.entity.JiangshiType;
import com.heishan.jiangshidao.event.AccessorySyncHandler;
import com.heishan.jiangshidao.event.LightningIgniteHandler;
import com.heishan.jiangshidao.event.PouchInteractionHandler;
import com.heishan.jiangshidao.event.SealedEffectHandler;
import com.heishan.jiangshidao.event.ChunyangJinHandler;
import com.heishan.jiangshidao.event.SubduedEffectHandler;
import com.heishan.jiangshidao.event.SunHatProtectionHandler;
import com.heishan.jiangshidao.event.TalentEffectHandler;
import com.heishan.jiangshidao.event.WarCombatHandler;
import com.heishan.jiangshidao.event.WorkerTrampleHandler;
import com.heishan.jiangshidao.network.ModNetwork;
import com.heishan.jiangshidao.network.PouchQuickMoveHandler;
import com.heishan.jiangshidao.registry.ModBlockEntities;
import com.heishan.jiangshidao.registry.ModBlocks;
import com.heishan.jiangshidao.registry.ModCreativeTabs;
import com.heishan.jiangshidao.registry.ModDataComponents;
import com.heishan.jiangshidao.registry.ModEffects;
import com.heishan.jiangshidao.registry.ModEntities;
import com.heishan.jiangshidao.registry.ModFeatures;
import com.heishan.jiangshidao.registry.ModItems;
import com.heishan.jiangshidao.registry.ModMenus;
import com.heishan.jiangshidao.registry.ModTags;
import com.heishan.jiangshidao.worker.WorkerHandler;
import java.util.function.Predicate;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.EndTick;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * JiangshiDaoFabric。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class JiangshiDaoFabric implements ModInitializer {
   public void onInitialize() {
      JiangshiDao.LOGGER.info("僵尸道 Jiangshi Dao (Fabric) 正在初始化");
      safe("ModDataComponents", ModDataComponents::touch);
      safe("ModEffects", ModEffects::touch);
      safe("ModBlocks", ModBlocks::touch);
      safe("ModItems", ModItems::touch);
      safe("ModBlockEntities", ModBlockEntities::touch);
      safe("ModEntities", ModEntities::touch);
      safe("ModMenus", ModMenus::touch);
      safe("ModFeatures", ModFeatures::touch);
      safe("ModTags", ModTags::touch);
      safe("ModCreativeTabs", ModCreativeTabs::touch);
      safe("network", ModNetwork::register);
      safe("network.PouchQuickMove", PouchQuickMoveHandler::register);
      safe("attributes", JiangshiDaoFabric::registerAttributes);
      safe("spawnPlacements", JiangshiDaoFabric::registerSpawnPlacements);
      safe("biomeSpawns", JiangshiDaoFabric::registerBiomeSpawns);
      safe("biomeFeatures", JiangshiDaoFabric::registerBiomeFeatures);
      safe("event.AccessorySync", AccessorySyncHandler::register);
      safe("event.LightningIgnite", LightningIgniteHandler::register);
      safe("event.ChunyangJin", ChunyangJinHandler::register);
      safe("event.PouchInteraction", PouchInteractionHandler::register);
      safe("event.SealedEffect", SealedEffectHandler::register);
      safe("event.SubduedEffect", SubduedEffectHandler::register);
      safe("event.SunHatProtection", SunHatProtectionHandler::register);
      safe("event.TalentEffect", TalentEffectHandler::register);
      safe("event.WarCombat", WarCombatHandler::register);
      safe("event.WorkerTrample", WorkerTrampleHandler::register);
      safe("selfTest", SelfTest::register);
      ServerTickEvents.END_SERVER_TICK.register((EndTick)server -> {
         for (ServerLevel level : server.getAllLevels()) {
            try {
               WorkerHandler.tickLevel(level);
            } catch (Throwable t) {
               JiangshiDao.LOGGER.error("[工人] tick 异常", t);
            }
         }
      });
      JiangshiDao.LOGGER.info("僵尸道 Jiangshi Dao (Fabric) 初始化完成");
   }

   private static void safe(String step, Runnable action) {
      try {
         action.run();
      } catch (Throwable t) {
         JiangshiDao.LOGGER.error("[初始化] 步骤 " + step + " 失败", t);
      }
   }

   private static void registerAttributes() {
      for (JiangshiType type : JiangshiType.values()) {
         FabricDefaultAttributeRegistry.register(ModEntities.get(type), JiangshiEntity.createAttributesInternal(type));
      }

      for (GhostWomanVariant variant : GhostWomanVariant.values()) {
         FabricDefaultAttributeRegistry.register(ModEntities.ghost(variant), GhostWomanEntity.createAttributes(variant));
      }
   }

   private static void registerSpawnPlacements() {
      for (JiangshiType type : JiangshiType.values()) {
         SpawnPlacements.register(ModEntities.get(type), SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
      }

      for (GhostWomanVariant variant : GhostWomanVariant.values()) {
         SpawnPlacements.register(ModEntities.ghost(variant), SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
      }
   }

   private static void registerBiomeSpawns() {
      addSpawn(JiangshiType.TEAL, 30, 1, 2);
      addSpawn(JiangshiType.PURPLE, 30, 1, 2);
      addSpawn(JiangshiType.YELLOW, 20, 1, 1);
      addSpawn(JiangshiType.BRUTE, 8, 1, 1);
      addGhostSpawn(GhostWomanVariant.BLUE, 12);
      addGhostSpawn(GhostWomanVariant.RED, 8);
      addGhostSpawn(GhostWomanVariant.WHITE, 8);
   }

   private static void addSpawn(JiangshiType type, int weight, int min, int max) {
      BiomeModifications.addSpawn(overworld(), MobCategory.MONSTER, ModEntities.get(type), weight, min, max);
   }

   private static void addGhostSpawn(GhostWomanVariant variant, int weight) {
      BiomeModifications.addSpawn(overworld(), MobCategory.MONSTER, ModEntities.ghost(variant), weight, 1, 1);
   }

   private static Predicate<BiomeSelectionContext> overworld() {
      return BiomeSelectors.tag(BiomeTags.IS_OVERWORLD);
   }

   private static void registerBiomeFeatures() {
      for (String id : new String[]{"sulfur_upper", "sulfur_lower", "cinnabar_upper", "cinnabar_lower"}) {
         BiomeModifications.addFeature(overworld(), Decoration.UNDERGROUND_ORES, placedFeature(id));
      }

      BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_FOREST), Decoration.VEGETAL_DECORATION, placedFeature("peach_tree"));
      BiomeModifications.addFeature(
         BiomeSelectors.includeByKey(new ResourceKey[]{Biomes.CHERRY_GROVE}), Decoration.VEGETAL_DECORATION, placedFeature("peach_tree")
      );
   }

   private static ResourceKey<PlacedFeature> placedFeature(String path) {
      return ResourceKey.create(Registries.PLACED_FEATURE, JiangshiDao.id(path));
   }
}
