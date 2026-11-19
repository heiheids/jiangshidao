package com.heishan.jiangshidao.item;

import com.heishan.jiangshidao.accessory.Accessories;
import com.heishan.jiangshidao.accessory.AccessorySlot;
import com.heishan.jiangshidao.registry.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * 纯阳巾（道冠）。
 *
 * <p>装备在模组自带饰品栏的头部槽，渲染在玩家头顶。
 *
 * <p>三项能力，<b>都只对佩戴者生效，且都需要累计佩戴满 {@link #ACTIVATE_TICKS} 才激活</b>：
 * <ol>
 *   <li>看见 25 格内的隐身女鬼（仅佩戴者可见，其他玩家照旧看不见）</li>
 *   <li>击杀亡灵生物时，掉落物与经验各翻一倍</li>
 *   <li>原版亡灵生物不主动攻击佩戴者；佩戴者先动手则正常还击</li>
 * </ol>
 *
 * <p>累计佩戴时长记在物品自身的数据组件上，换下再戴回进度不丢。
 *
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class ChunyangJinItem extends Item {
   /** 一个游戏日的 tick 数。 */
   public static final int TICKS_PER_DAY = 24000;
   /** 激活所需的累计佩戴时长：10 个游戏日。 */
   public static final int ACTIVATE_DAYS = 10;
   public static final int ACTIVATE_TICKS = TICKS_PER_DAY * ACTIVATE_DAYS;
   /**
    * 验收用门槛覆盖：通过启动参数 {@code -Djiangshidao.chunyang.activate=200} 临时降低门槛。
    *
    * <p>默认（不传参数）严格使用 {@link #ACTIVATE_TICKS}，即 10 个游戏日，
    * 符合设计规格；只有显式传参才会缩短，方便快速验证三项能力。
    */
   private static final int TEST_ACTIVATE_TICKS = readIntProperty("jiangshidao.chunyang.activate", 0);
   /** 实际生效的门槛（启动参数优先，默认 10 游戏日）。 */
   public static int activateTicks() {
      return TEST_ACTIVATE_TICKS > 0 ? TEST_ACTIVATE_TICKS : ACTIVATE_TICKS;
   }

   /** 读取可覆盖的整数启动参数。 */
   private static int readIntProperty(String key, int fallback) {
      try {
         String raw = System.getProperty(key);
         if (raw != null && !raw.isBlank()) {
            return Integer.parseInt(raw.trim());
         }
      } catch (Throwable ignored) {
         // 参数写错就用默认值
      }

      return fallback;
   }
   /** 女鬼显形半径（格）。 */
   public static final double REVEAL_RADIUS = 25.0;
   public static final double REVEAL_RADIUS_SQR = REVEAL_RADIUS * REVEAL_RADIUS;

   public ChunyangJinItem(Properties properties) {
      super(properties);
   }

   /** 物品上的累计佩戴 tick，缺失按 0。 */
   public static int wearTicks(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return 0;
      }

      Integer value = stack.get(ModDataComponents.CHUNYANG_WEAR.get());
      return value == null || value < 0 ? 0 : value;
   }

   /** 写入累计佩戴 tick。 */
   public static void setWearTicks(ItemStack stack, int ticks) {
      if (stack != null && !stack.isEmpty()) {
         stack.set(ModDataComponents.CHUNYANG_WEAR.get(), Math.max(0, ticks));
      }
   }

   /** 该物品是否已经佩戴满 10 日、能力是否已激活。 */
   public static boolean isActive(ItemStack stack) {
      return wearTicks(stack) >= activateTicks();
   }

   /**
    * 玩家当前是否戴着<b>已激活</b>的纯阳巾。
    *
    * <p>各能力系统统一通过本方法判定，避免各处重复读槽位。
    */
   public static boolean isWearingActive(Player player) {
      if (player == null) {
         return false;
      }

      ItemStack head = Accessories.peek(player).get(AccessorySlot.HEAD);
      return head.getItem() instanceof ChunyangJinItem && isActive(head);
   }

   /** 玩家是否戴着纯阳巾（无论是否已激活）。 */
   public static boolean isWearing(Player player) {
      if (player == null) {
         return false;
      }

      return Accessories.peek(player).get(AccessorySlot.HEAD).getItem() instanceof ChunyangJinItem;
   }

   public void appendHoverText(
      ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag
   ) {
      ChatFormatting desc = ChatFormatting.GRAY;
      ChatFormatting italic = ChatFormatting.ITALIC;

      tooltip.accept(descText("tooltip.jiangshidao.chunyang_jin.desc", desc, italic));
      tooltip.accept(descText("tooltip.jiangshidao.chunyang_jin.effect.reveal", ChatFormatting.AQUA, null));
      tooltip.accept(descText("tooltip.jiangshidao.chunyang_jin.effect.loot", ChatFormatting.AQUA, null));
      tooltip.accept(descText("tooltip.jiangshidao.chunyang_jin.effect.peace", ChatFormatting.AQUA, null));

      int ticks = wearTicks(stack);
      if (isActive(stack)) {
         tooltip.accept(Component.translatable("tooltip.jiangshidao.chunyang_jin.wear.ready").withStyle(ChatFormatting.GOLD));
      } else {
         // 显示到 0.1 日，让玩家能看出进度在动
         int daysTenths = (int)((long)ticks * 10L / TICKS_PER_DAY);
         int percent = (int)((long)ticks * 100L / activateTicks());
         tooltip.accept(
            Component.translatable(
                  "tooltip.jiangshidao.chunyang_jin.wear.progress",
                  String.format("%.1f", daysTenths / 10.0),
                  String.valueOf(ACTIVATE_DAYS),
                  String.valueOf(Math.min(100, percent)))
               .withStyle(ChatFormatting.YELLOW)
         );
      }

      tooltip.accept(descText("tooltip.jiangshidao.chunyang_jin.advice", ChatFormatting.LIGHT_PURPLE, italic));
   }

   private static Component descText(String key, ChatFormatting color, ChatFormatting extra) {
      net.minecraft.network.chat.Style style = net.minecraft.network.chat.Style.EMPTY.withColor(color);
      if (extra != null) {
         style = style.applyFormat(extra);
      }

      return Component.translatable(key).setStyle(style);
   }
}
