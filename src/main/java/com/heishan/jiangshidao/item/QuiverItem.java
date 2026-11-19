package com.heishan.jiangshidao.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * QuiverItem —— 物品行为（使用、命中、提示等）。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class QuiverItem extends Item {
   public static final int SLOTS = 3;
   public static final float COOLDOWN_MULTIPLIER = 0.6666667F;

   public QuiverItem(Properties properties) {
      super(properties);
   }

   public static boolean acceptsSword(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() instanceof WoodSwordItem;
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, display, tooltip, flag);
      tooltip.accept(Component.translatable("tooltip.jiangshidao.quiver.desc").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
      tooltip.accept(Component.translatable("tooltip.jiangshidao.quiver.throw").withStyle(ChatFormatting.GOLD));
      tooltip.accept(Component.translatable("tooltip.jiangshidao.quiver.cooldown").withStyle(ChatFormatting.AQUA));
   }
}
