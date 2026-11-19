package com.heishan.jiangshidao.registry;

import com.heishan.jiangshidao.compat.Reg;
import com.heishan.jiangshidao.compat.RegistryHelper;
import com.heishan.jiangshidao.entity.GhostWomanVariant;
import com.heishan.jiangshidao.entity.JiangshiType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab.Row;
import net.minecraft.world.level.ItemLike;

/**
 * ModCreativeTabs。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ModCreativeTabs {
   public static final String TAB_TITLE_KEY = "itemGroup.jiangshidao.main";
   public static final Reg<CreativeModeTab> JIANGSHI_DAO_TAB = RegistryHelper.reg(
      BuiltInRegistries.CREATIVE_MODE_TAB,
      "main",
      () -> CreativeModeTab.builder(Row.TOP, 0)
         .icon(() -> new ItemStack(ModItems.spawnEgg(JiangshiType.TEAL)))
         .title(Component.translatable("itemGroup.jiangshidao.main"))
         .displayItems((parameters, output) -> {
            output.accept((ItemLike)ModBlocks.STONE_MILL_ITEM.get());
            output.accept((ItemLike)ModBlocks.INKSTONE_ITEM.get());
            output.accept((ItemLike)ModItems.SULFUR_POWDER.get());
            output.accept((ItemLike)ModItems.CINNABAR_POWDER.get());
            output.accept((ItemLike)ModItems.BRUSH.get());
            output.accept((ItemLike)ModItems.SEALING_TALISMAN.get());
            output.accept((ItemLike)ModItems.PEACH_LOG.get());
            output.accept((ItemLike)ModItems.PEACH_WOOD.get());
            output.accept((ItemLike)ModItems.PEACH_PLANKS.get());
            output.accept((ItemLike)ModItems.PEACH_LEAVES.get());
         output.accept((ItemLike)ModItems.PEACH.get());
            output.accept((ItemLike)ModItems.PEACH_SAPLING.get());
            output.accept((ItemLike)ModItems.LEI_JI_MU.get());
            output.accept((ItemLike)ModItems.TIAN_FA_LEI_JI_MU.get());
            output.accept((ItemLike)ModItems.LEI_JI_MU_PLANKS.get());
            output.accept((ItemLike)ModItems.TIAN_FA_LEI_JI_MU_PLANKS.get());
            output.accept((ItemLike)ModItems.BLANK_TALISMAN_PAPER.get());
            output.accept((ItemLike)ModItems.BURNING_TALISMAN.get());
            output.accept((ItemLike)ModItems.THUNDER_TALISMAN.get());
            output.accept((ItemLike)ModItems.TALISMAN_POUCH.get());
            output.accept((ItemLike)ModItems.PEACH_WOOD_SWORD.get());
            output.accept((ItemLike)ModItems.THUNDER_WOOD_SWORD.get());
            output.accept((ItemLike)ModItems.TIAN_FA_LEI_JI_MU_SWORD.get());
            output.accept((ItemLike)ModItems.QUIVER.get());
         output.accept((ItemLike)ModItems.CHUNYANG_JIN.get());
            output.accept((ItemLike)ModItems.SUBJUGATION_TALISMAN.get());
            output.accept((ItemLike)ModItems.BRONZE_BELL.get());

            for (JiangshiType type : JiangshiType.values()) {
               output.accept(ModItems.spawnEgg(type));
            }

            for (GhostWomanVariant v : GhostWomanVariant.values()) {
               output.accept(ModItems.ghostWomanSpawnEgg(v));
            }
         })
         .build()
   );

   public static void touch() {
   }

   private ModCreativeTabs() {
   }
}
