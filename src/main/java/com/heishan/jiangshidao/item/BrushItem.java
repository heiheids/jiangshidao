package com.heishan.jiangshidao.item;

import com.heishan.jiangshidao.registry.ModDataComponents;
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
 * BrushItem —— 物品行为（使用、命中、提示等）。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class BrushItem extends Item {
   public BrushItem(Properties properties) {
      super(properties);
   }

   public static boolean isInked(ItemStack stack) {
      return Boolean.TRUE.equals(stack.get(ModDataComponents.INKED.get()));
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, display, tooltip, flag);
      if (isInked(stack)) {
         tooltip.accept(Component.translatable("tooltip.jiangshidao.brush.inked").withStyle(ChatFormatting.RED));
      } else {
         tooltip.accept(Component.translatable("tooltip.jiangshidao.brush.dry").withStyle(ChatFormatting.GRAY));
      }

      tooltip.accept(Component.translatable("tooltip.jiangshidao.brush.usage").withStyle(ChatFormatting.DARK_GRAY));
   }

   public boolean isFoil(ItemStack stack) {
      return isInked(stack);
   }
}
