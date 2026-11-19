package com.heishan.jiangshidao.item;

import com.heishan.jiangshidao.entity.SubjugationTalismanEntity;
import com.heishan.jiangshidao.registry.ModEffects;
import com.heishan.jiangshidao.worker.WorkerData;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
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
 * SubjugationTalismanItem —— 物品行为（使用、命中、提示等）。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class SubjugationTalismanItem extends Item {
   public static final float BASE_CHANCE = 0.05F;
   public static final float WEAKENED_CHANCE = 0.25F;
   public static final float WEAKENED_HEALTH_RATIO = 0.1F;
   public static final int COOLDOWN_TICKS = 60;
   public static final int SUBDUE_DURATION = -1;

   public SubjugationTalismanItem(Properties properties) {
      super(properties);
   }

   public static boolean isSubduable(LivingEntity target) {
      return target.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD);
   }

   public static float chanceFor(LivingEntity target) {
      float ratio = target.getHealth() / Math.max(1.0F, target.getMaxHealth());
      return ratio <= 0.1F ? 0.25F : 0.05F;
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (player.getCooldowns().isOnCooldown(stack)) {
         return InteractionResult.FAIL;
      }

      player.getCooldowns().addCooldown(stack, 60);
      if (!level.isClientSide()) {
         SubjugationTalismanEntity talisman = new SubjugationTalismanEntity(level, player);
         talisman.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
         level.addFreshEntity(talisman);
      }

      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.6F, 1.4F);
      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }

      return InteractionResult.SUCCESS;
   }

   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
      if (player.level().isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      if (isSubduable(target) && !ModEffects.isSubdued(target)) {
         if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
         }

         player.getCooldowns().addCooldown(stack, 60);
         float chance = chanceFor(target);
         if (player.getRandom().nextFloat() < chance) {
            WorkerData.ensureName(target);
            target.addEffect(new MobEffectInstance(ModEffects.SUBDUED, -1, 0, false, true, true));
            WorkerData.setOwner(target, player.getUUID());
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 0.9F, 1.4F);
            player.sendOverlayMessage(Component.translatable("message.jiangshidao.subjugation.success"));
         } else {
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 0.5F, 0.7F);
            player.sendOverlayMessage(Component.translatable("message.jiangshidao.subjugation.fail"));
         }

         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
         }

         return InteractionResult.CONSUME;
      } else {
         return InteractionResult.PASS;
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, display, tooltip, flag);
      tooltip.accept(
         Component.translatable("tooltip.jiangshidao.subjugation_talisman.desc").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC})
      );
      tooltip.accept(Component.translatable("tooltip.jiangshidao.subjugation_talisman.chance").withStyle(ChatFormatting.GOLD));
      tooltip.accept(Component.translatable("tooltip.jiangshidao.subjugation_talisman.cooldown").withStyle(ChatFormatting.AQUA));
   }
}
