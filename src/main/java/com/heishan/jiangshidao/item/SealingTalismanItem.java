package com.heishan.jiangshidao.item;

import com.heishan.jiangshidao.entity.SealingTalismanEntity;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * SealingTalismanItem —— 物品行为（使用、命中、提示等）。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class SealingTalismanItem extends Item {
   private static final float THROW_SPEED = 1.5F;

   public SealingTalismanItem(Properties properties) {
      super(properties);
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      level.playSound(
         null,
         player.getX(),
         player.getY(),
         player.getZ(),
         SoundEvents.SNOWBALL_THROW,
         SoundSource.PLAYERS,
         0.5F,
         0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
      );
      if (!level.isClientSide()) {
         SealingTalismanEntity talisman = new SealingTalismanEntity(level, player);
         talisman.shootFromRotation(player, player.getXRot() - 3.0F, player.getYRot(), 0.0F, 1.5F, 0.4F);
         level.addFreshEntity(talisman);
      }

      player.awardStat(Stats.ITEM_USED.get(this));
      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }

      return InteractionResult.SUCCESS;
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, display, tooltip, flag);
      tooltip.accept(Component.translatable("tooltip.jiangshidao.sealing_talisman").withStyle(ChatFormatting.GOLD));
      tooltip.accept(Component.translatable("tooltip.jiangshidao.sealing_talisman.detail").withStyle(ChatFormatting.GRAY));
      tooltip.accept(Component.translatable("tooltip.jiangshidao.talisman.nocooldown").withStyle(ChatFormatting.DARK_GREEN));
   }

   public static boolean isUndead(LivingEntity entity) {
      return entity.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD);
   }
}
