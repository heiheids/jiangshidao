package com.heishan.jiangshidao.registry;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.block.BlockProps;
import com.heishan.jiangshidao.block.BronzeBellBlock;
import com.heishan.jiangshidao.block.InkstoneBlock;
import com.heishan.jiangshidao.block.LightningLogBlock;
import com.heishan.jiangshidao.block.PeachSaplingBlock;
import com.heishan.jiangshidao.block.StoneMillBlock;
import com.heishan.jiangshidao.block.TalismanPaperBlock;
import com.heishan.jiangshidao.compat.Reg;
import com.heishan.jiangshidao.compat.RegistryHelper;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

/**
 * ModBlocks。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ModBlocks {
   public static final Reg<Block> STONE_MILL = RegistryHelper.reg(BuiltInRegistries.BLOCK, "stone_mill", StoneMillBlock::new);
   public static final Reg<Block> INKSTONE = RegistryHelper.reg(BuiltInRegistries.BLOCK, "inkstone", () -> new InkstoneBlock());
   public static final Reg<Item> STONE_MILL_ITEM = RegistryHelper.reg(
      BuiltInRegistries.ITEM, "stone_mill", () -> new BlockItem(STONE_MILL.get(), itemProps("stone_mill"))
   );
   public static final Reg<Item> INKSTONE_ITEM = RegistryHelper.reg(
      BuiltInRegistries.ITEM, "inkstone", () -> new BlockItem(INKSTONE.get(), itemProps("inkstone"))
   );
   public static final Reg<Block> TALISMAN_PAPER = RegistryHelper.reg(
      BuiltInRegistries.BLOCK, "talisman_paper", () -> new TalismanPaperBlock(TalismanPaperBlock.defaultProperties())
   );
   public static final Reg<Block> PEACH_LOG = RegistryHelper.reg(
      BuiltInRegistries.BLOCK,
      "peach_log",
      () -> new LightningLogBlock(
         Properties.ofFullCopy(Blocks.OAK_LOG).setId(ResourceKey.create(Registries.BLOCK, JiangshiDao.id("peach_log"))).randomTicks(),
         new LightningLogBlock.BurnProfile(0.1F, () -> ModBlocks.LEI_JI_MU.get(), 0.0F, null)
      )
   );
   public static final Reg<Block> LEI_JI_MU = RegistryHelper.reg(
      BuiltInRegistries.BLOCK,
      "lei_ji_mu",
      () -> new LightningLogBlock(
         Properties.ofFullCopy(Blocks.OAK_LOG).setId(ResourceKey.create(Registries.BLOCK, JiangshiDao.id("lei_ji_mu"))).randomTicks(),
         new LightningLogBlock.BurnProfile(0.1F, () -> ModBlocks.LEI_JI_MU.get(), 0.01F, () -> ModBlocks.TIAN_FA_LEI_JI_MU.get())
      )
   );
   public static final Reg<Block> TIAN_FA_LEI_JI_MU = RegistryHelper.reg(
      BuiltInRegistries.BLOCK,
      "tian_fa_lei_ji_mu",
      () -> new LightningLogBlock(
         Properties.ofFullCopy(Blocks.OAK_LOG)
            .setId(ResourceKey.create(Registries.BLOCK, JiangshiDao.id("tian_fa_lei_ji_mu")))
            .lightLevel(state -> 10)
            .randomTicks(),
         null
      )
   );
   public static final Reg<Block> PEACH_WOOD = RegistryHelper.reg(
      BuiltInRegistries.BLOCK,
      "peach_wood",
      () -> new RotatedPillarBlock(Properties.ofFullCopy(Blocks.OAK_WOOD).setId(ResourceKey.create(Registries.BLOCK, JiangshiDao.id("peach_wood"))))
   );
   public static final Reg<Block> PEACH_PLANKS = RegistryHelper.reg(
      BuiltInRegistries.BLOCK,
      "peach_planks",
      () -> new Block(Properties.ofFullCopy(Blocks.OAK_PLANKS).setId(ResourceKey.create(Registries.BLOCK, JiangshiDao.id("peach_planks"))))
   );
   public static final Reg<Block> PEACH_LEAVES = RegistryHelper.reg(
      BuiltInRegistries.BLOCK,
      "peach_leaves",
      () -> new com.heishan.jiangshidao.block.PeachLeavesBlock(
         new AmbientLeavesBlockSoundPlayer(Optional.empty(), 300, Optional.empty(), 1, 3),
         Properties.ofFullCopy(Blocks.OAK_LEAVES)
            .randomTicks()
            .setId(ResourceKey.create(Registries.BLOCK, JiangshiDao.id("peach_leaves")))
      )
   );
   public static final Reg<Block> PEACH_SAPLING = RegistryHelper.reg(
      BuiltInRegistries.BLOCK,
      "peach_sapling",
      () -> new PeachSaplingBlock(Properties.ofFullCopy(Blocks.OAK_SAPLING).setId(ResourceKey.create(Registries.BLOCK, JiangshiDao.id("peach_sapling"))))
   );
   public static final Reg<Block> LEI_JI_MU_PLANKS = RegistryHelper.reg(
      BuiltInRegistries.BLOCK,
      "lei_ji_mu_planks",
      () -> new Block(Properties.ofFullCopy(Blocks.OAK_PLANKS).setId(ResourceKey.create(Registries.BLOCK, JiangshiDao.id("lei_ji_mu_planks"))))
   );
   public static final Reg<Block> TIAN_FA_LEI_JI_MU_PLANKS = RegistryHelper.reg(
      BuiltInRegistries.BLOCK,
      "tian_fa_lei_ji_mu_planks",
      () -> new Block(Properties.ofFullCopy(Blocks.OAK_PLANKS).setId(ResourceKey.create(Registries.BLOCK, JiangshiDao.id("tian_fa_lei_ji_mu_planks"))))
   );
   public static final Reg<Block> BRONZE_BELL = RegistryHelper.reg(
      BuiltInRegistries.BLOCK, "bronze_bell", () -> new BronzeBellBlock(BlockProps.of("bronze_bell").strength(0.5F).noOcclusion().sound(SoundType.METAL))
   );

   public static void touch() {
   }

   private static net.minecraft.world.item.Item.Properties itemProps(String name) {
      return new net.minecraft.world.item.Item.Properties().setId(ResourceKey.create(Registries.ITEM, JiangshiDao.id(name)));
   }

   private ModBlocks() {
   }

   public static Block stoneMill() {
      return STONE_MILL.get();
   }
}
