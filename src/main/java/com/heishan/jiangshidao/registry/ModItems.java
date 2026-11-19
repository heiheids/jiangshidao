package com.heishan.jiangshidao.registry;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.compat.Reg;
import com.heishan.jiangshidao.compat.RegistryHelper;
import com.heishan.jiangshidao.entity.GhostWomanVariant;
import com.heishan.jiangshidao.entity.JiangshiType;
import com.heishan.jiangshidao.item.BronzeBellItem;
import com.heishan.jiangshidao.item.BrushItem;
import com.heishan.jiangshidao.item.BurningTalismanItem;
import com.heishan.jiangshidao.item.ChunyangJinItem;
import com.heishan.jiangshidao.item.QuiverItem;
import com.heishan.jiangshidao.item.SealingTalismanItem;
import com.heishan.jiangshidao.item.SubjugationTalismanItem;
import com.heishan.jiangshidao.item.TalismanPouchItem;
import com.heishan.jiangshidao.item.ThunderTalismanItem;
import com.heishan.jiangshidao.item.TooltipBlockItem;
import com.heishan.jiangshidao.item.WoodSwordItem;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.TypedEntityData;

/**
 * ModItems。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ModItems {
   public static final Map<JiangshiType, Reg<Item>> SPAWN_EGGS = new EnumMap<>(JiangshiType.class);
   public static final Map<GhostWomanVariant, Reg<Item>> GHOST_WOMAN_EGGS;
   public static final Reg<Item> SULFUR_POWDER;
   public static final Reg<Item> CINNABAR_POWDER;
   public static final Reg<Item> BRUSH;
   public static final Reg<Item> SEALING_TALISMAN;
   public static final Reg<Item> PEACH_WOOD_SWORD;
   public static final Reg<Item> THUNDER_WOOD_SWORD;
   public static final Reg<Item> TIAN_FA_LEI_JI_MU_SWORD;
   public static final Reg<Item> TALISMAN_POUCH;
   public static final Reg<Item> BURNING_TALISMAN;
   public static final Reg<Item> THUNDER_TALISMAN;
   public static final Reg<Item> BLANK_TALISMAN_PAPER;
   public static final Reg<Item> PEACH_LOG;
   public static final Reg<Item> PEACH_WOOD;
   public static final Reg<Item> PEACH_PLANKS;
   public static final Reg<Item> PEACH_LEAVES;
   /** 桃子：食物，效果为普通苹果的 2 倍。 */
   public static final Reg<Item> PEACH;
   public static final Reg<Item> PEACH_SAPLING;
   public static final Reg<Item> LEI_JI_MU;
   public static final Reg<Item> TIAN_FA_LEI_JI_MU;
   public static final Reg<Item> LEI_JI_MU_PLANKS;
   public static final Reg<Item> SUBJUGATION_TALISMAN;
   public static final Reg<Item> BRONZE_BELL;
   public static final Reg<Item> QUIVER;
   public static final Reg<Item> CHUNYANG_JIN;
   public static final Reg<Item> TIAN_FA_LEI_JI_MU_PLANKS;

   public static void touch() {
   }

   private static Properties props(String name) {
      return new Properties().setId(ResourceKey.create(Registries.ITEM, JiangshiDao.id(name)));
   }

   private ModItems() {
   }

   public static Item spawnEgg(JiangshiType type) {
      return SPAWN_EGGS.get(type).get();
   }

   public static Item ghostWomanSpawnEgg(GhostWomanVariant variant) {
      return GHOST_WOMAN_EGGS.get(variant).get();
   }

   public static Item ghostWomanSpawnEgg() {
      return ghostWomanSpawnEgg(GhostWomanVariant.BLUE);
   }

   static {
      for (JiangshiType type : JiangshiType.values()) {
         Reg<Item> holder = RegistryHelper.reg(
            BuiltInRegistries.ITEM,
            type.eggPath(),
            () -> new SpawnEggItem(props(type.eggPath()).component(DataComponents.ENTITY_DATA, TypedEntityData.of(ModEntities.get(type), new CompoundTag())))
         );
         SPAWN_EGGS.put(type, holder);
      }

      GHOST_WOMAN_EGGS = new EnumMap<>(GhostWomanVariant.class);

      for (GhostWomanVariant variant : GhostWomanVariant.values()) {
         Reg<Item> holder = RegistryHelper.reg(
            BuiltInRegistries.ITEM,
            variant.eggPath(),
            () -> new SpawnEggItem(
               props(variant.eggPath()).component(DataComponents.ENTITY_DATA, TypedEntityData.of(ModEntities.ghost(variant), new CompoundTag()))
            )
         );
         GHOST_WOMAN_EGGS.put(variant, holder);
      }

      SULFUR_POWDER = RegistryHelper.reg(BuiltInRegistries.ITEM, "sulfur_powder", () -> new Item(props("sulfur_powder").stacksTo(64)));
      CINNABAR_POWDER = RegistryHelper.reg(BuiltInRegistries.ITEM, "cinnabar_powder", () -> new Item(props("cinnabar_powder").stacksTo(64)));
      BRUSH = RegistryHelper.reg(BuiltInRegistries.ITEM, "brush", () -> new BrushItem(props("brush").stacksTo(1)));
      SEALING_TALISMAN = RegistryHelper.reg(BuiltInRegistries.ITEM, "sealing_talisman", () -> new SealingTalismanItem(props("sealing_talisman").stacksTo(64)));
      PEACH_WOOD_SWORD = RegistryHelper.reg(
         BuiltInRegistries.ITEM,
         "peach_wood_sword",
         () -> new WoodSwordItem(
            WoodSwordItem.Variant.PEACH,
            props("peach_wood_sword").stacksTo(1).sword(WoodSwordItem.WOOD_TIER, 3.0F + WoodSwordItem.Variant.PEACH.attackBonus(), -2.4F)
         )
      );
      THUNDER_WOOD_SWORD = RegistryHelper.reg(
         BuiltInRegistries.ITEM,
         "thunder_wood_sword",
         () -> new WoodSwordItem(
            WoodSwordItem.Variant.THUNDER,
            props("thunder_wood_sword").stacksTo(1).sword(WoodSwordItem.WOOD_TIER, 3.0F + WoodSwordItem.Variant.THUNDER.attackBonus(), -2.4F)
         )
      );
      TIAN_FA_LEI_JI_MU_SWORD = RegistryHelper.reg(
         BuiltInRegistries.ITEM,
         "tian_fa_lei_ji_mu_sword",
         () -> new WoodSwordItem(
            WoodSwordItem.Variant.TIAN_FA,
            props("tian_fa_lei_ji_mu_sword").stacksTo(1).sword(WoodSwordItem.WOOD_TIER, 3.0F + WoodSwordItem.Variant.TIAN_FA.attackBonus(), -2.4F)
         )
      );
      TALISMAN_POUCH = RegistryHelper.reg(BuiltInRegistries.ITEM, "talisman_pouch", () -> new TalismanPouchItem(props("talisman_pouch").stacksTo(1)));
      BURNING_TALISMAN = RegistryHelper.reg(BuiltInRegistries.ITEM, "burning_talisman", () -> new BurningTalismanItem(props("burning_talisman").stacksTo(64)));
      THUNDER_TALISMAN = RegistryHelper.reg(BuiltInRegistries.ITEM, "thunder_talisman", () -> new ThunderTalismanItem(props("thunder_talisman").stacksTo(64)));
      BLANK_TALISMAN_PAPER = RegistryHelper.reg(
         BuiltInRegistries.ITEM, "blank_talisman_paper", () -> new BlockItem(ModBlocks.TALISMAN_PAPER.get(), props("blank_talisman_paper").stacksTo(64))
      );
      PEACH_LOG = RegistryHelper.reg(BuiltInRegistries.ITEM, "peach_log", () -> new BlockItem(ModBlocks.PEACH_LOG.get(), props("peach_log")));
      PEACH_WOOD = RegistryHelper.reg(BuiltInRegistries.ITEM, "peach_wood", () -> new BlockItem(ModBlocks.PEACH_WOOD.get(), props("peach_wood")));
      PEACH_PLANKS = RegistryHelper.reg(BuiltInRegistries.ITEM, "peach_planks", () -> new BlockItem(ModBlocks.PEACH_PLANKS.get(), props("peach_planks")));
      PEACH_LEAVES = RegistryHelper.reg(BuiltInRegistries.ITEM, "peach_leaves", () -> new BlockItem(ModBlocks.PEACH_LEAVES.get(), props("peach_leaves")));
   // 苹果是 4 饱食度 / 2.4 饱和度，桃子取其两倍。
   PEACH = RegistryHelper.reg(
      BuiltInRegistries.ITEM,
      "peach",
      () -> new Item(props("peach").stacksTo(64).food(new net.minecraft.world.food.FoodProperties(8, 4.8F, false)))
   );
      PEACH_SAPLING = RegistryHelper.reg(BuiltInRegistries.ITEM, "peach_sapling", () -> new BlockItem(ModBlocks.PEACH_SAPLING.get(), props("peach_sapling")));
      LEI_JI_MU = RegistryHelper.reg(
         BuiltInRegistries.ITEM, "lei_ji_mu", () -> new TooltipBlockItem(ModBlocks.LEI_JI_MU.get(), props("lei_ji_mu"), "tooltip.jiangshidao.lei_ji_mu.desc")
      );
      TIAN_FA_LEI_JI_MU = RegistryHelper.reg(
         BuiltInRegistries.ITEM,
         "tian_fa_lei_ji_mu",
         () -> new TooltipBlockItem(ModBlocks.TIAN_FA_LEI_JI_MU.get(), props("tian_fa_lei_ji_mu"), "tooltip.jiangshidao.tian_fa_lei_ji_mu.desc")
      );
      LEI_JI_MU_PLANKS = RegistryHelper.reg(
         BuiltInRegistries.ITEM, "lei_ji_mu_planks", () -> new BlockItem(ModBlocks.LEI_JI_MU_PLANKS.get(), props("lei_ji_mu_planks"))
      );
      SUBJUGATION_TALISMAN = RegistryHelper.reg(
         BuiltInRegistries.ITEM, "subjugation_talisman", () -> new SubjugationTalismanItem(props("subjugation_talisman").stacksTo(64))
      );
      BRONZE_BELL = RegistryHelper.reg(BuiltInRegistries.ITEM, "bronze_bell", () -> new BronzeBellItem(props("bronze_bell").stacksTo(1)));
      QUIVER = RegistryHelper.reg(BuiltInRegistries.ITEM, "quiver", () -> new QuiverItem(props("quiver").stacksTo(1)));
   CHUNYANG_JIN = RegistryHelper.reg(BuiltInRegistries.ITEM, "chunyang_jin", () -> new ChunyangJinItem(props("chunyang_jin").stacksTo(1)));
      TIAN_FA_LEI_JI_MU_PLANKS = RegistryHelper.reg(
         BuiltInRegistries.ITEM, "tian_fa_lei_ji_mu_planks", () -> new BlockItem(ModBlocks.TIAN_FA_LEI_JI_MU_PLANKS.get(), props("tian_fa_lei_ji_mu_planks"))
      );
   }
}
