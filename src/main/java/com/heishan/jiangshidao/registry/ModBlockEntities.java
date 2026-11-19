package com.heishan.jiangshidao.registry;

import com.heishan.jiangshidao.block.BronzeBellBlockEntity;
import com.heishan.jiangshidao.block.InkstoneBlockEntity;
import com.heishan.jiangshidao.block.StoneMillBlockEntity;
import com.heishan.jiangshidao.block.TalismanPaperBlockEntity;
import com.heishan.jiangshidao.compat.Reg;
import com.heishan.jiangshidao.compat.RegistryHelper;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * ModBlockEntities。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ModBlockEntities {
   public static final Reg<BlockEntityType<StoneMillBlockEntity>> STONE_MILL = RegistryHelper.reg(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, "stone_mill", () -> new BlockEntityType(StoneMillBlockEntity::new, Set.of(ModBlocks.STONE_MILL.get()))
   );
   public static final Reg<BlockEntityType<InkstoneBlockEntity>> INKSTONE = RegistryHelper.reg(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, "inkstone", () -> new BlockEntityType(InkstoneBlockEntity::new, Set.of(ModBlocks.INKSTONE.get()))
   );
   public static final Reg<BlockEntityType<TalismanPaperBlockEntity>> TALISMAN_PAPER = RegistryHelper.reg(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, "talisman_paper", () -> new BlockEntityType(TalismanPaperBlockEntity::new, Set.of(ModBlocks.TALISMAN_PAPER.get()))
   );
   public static final Reg<BlockEntityType<BronzeBellBlockEntity>> BRONZE_BELL = RegistryHelper.reg(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, "bronze_bell", () -> new BlockEntityType(BronzeBellBlockEntity::new, Set.of(ModBlocks.BRONZE_BELL.get()))
   );

   public static void touch() {
   }

   private ModBlockEntities() {
   }
}