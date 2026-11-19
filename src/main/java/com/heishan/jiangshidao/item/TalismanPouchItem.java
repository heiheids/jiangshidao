package com.heishan.jiangshidao.item;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.accessory.Accessories;
import com.heishan.jiangshidao.accessory.AccessorySlot;
import com.heishan.jiangshidao.entity.BurningTalismanEntity;
import com.heishan.jiangshidao.entity.SealingTalismanEntity;
import com.heishan.jiangshidao.entity.SubjugationTalismanEntity;
import com.heishan.jiangshidao.entity.ThunderTalismanEntity;
import com.heishan.jiangshidao.network.PouchSyncPayload;
import com.heishan.jiangshidao.registry.ModDataComponents;
import com.heishan.jiangshidao.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * TalismanPouchItem —— 物品行为（使用、命中、提示等）。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class TalismanPouchItem extends Item {
   public TalismanPouchItem(Properties properties) {
      super(properties);
   }

   public static List<ItemStack> getSlots(ItemStack pouch) {
      List<ItemStack> raw = (List<ItemStack>)pouch.get(ModDataComponents.POUCH_CONTENTS.get());
      List<ItemStack> out = new ArrayList<>(6);

      for (int i = 0; i < 6; i++) {
         if (raw != null && i < raw.size() && raw.get(i) != null) {
            out.add(raw.get(i));
         } else {
            out.add(ItemStack.EMPTY);
         }
      }

      return out;
   }

   public static void setSlots(ItemStack pouch, List<ItemStack> slots) {
      pouch.set(ModDataComponents.POUCH_CONTENTS.get(), new ArrayList<>(slots));
   }

   public static void setSlot(ItemStack pouch, int index, ItemStack value) {
      if (index >= 0 && index < 6) {
         List<ItemStack> slots = getSlots(pouch);
         slots.set(index, value == null ? ItemStack.EMPTY : value);
         setSlots(pouch, slots);
      }
   }

   public static boolean isEmpty(ItemStack pouch) {
      for (ItemStack s : getSlots(pouch)) {
         if (!s.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public static int usedSlots(ItemStack pouch) {
      int n = 0;

      for (ItemStack s : getSlots(pouch)) {
         if (!s.isEmpty()) {
            n++;
         }
      }

      return n;
   }

   public static boolean isValidContent(ItemStack stack) {
      return stack.isEmpty()
         ? false
         : stack.is(ModItems.SEALING_TALISMAN.get())
            || stack.is(ModItems.BURNING_TALISMAN.get())
            || stack.is(ModItems.THUNDER_TALISMAN.get())
            || stack.is(ModItems.SUBJUGATION_TALISMAN.get());
   }

   public static boolean tryInsert(ItemStack pouch, ItemStack toInsert) {
      if (!isValidContent(toInsert)) {
         return false;
      }

      List<ItemStack> slots = getSlots(pouch);

      for (int i = 0; i < slots.size(); i++) {
         ItemStack existing = slots.get(i);
         if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, toInsert)) {
            int space = existing.getMaxStackSize() - existing.getCount();
            if (space > 0) {
               int move = Math.min(space, toInsert.getCount());
               existing.grow(move);
               toInsert.shrink(move);
               setSlots(pouch, slots);
               return move > 0;
            }
         }
      }

      for (int i = 0; i < slots.size(); i++) {
         if (slots.get(i).isEmpty()) {
            slots.set(i, toInsert.copy());
            toInsert.setCount(0);
            setSlots(pouch, slots);
            return true;
         }
      }

      return false;
   }

   public static ItemStack findPouch(Player player) {
      ItemStack waist = Accessories.of(player).get(AccessorySlot.WAIST);
      if (waist.getItem() instanceof TalismanPouchItem) {
         return waist;
      }

      ItemStack off = player.getOffhandItem();
      if (off.getItem() instanceof TalismanPouchItem) {
         return off;
      }

      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack s = player.getInventory().getItem(i);
         if (s.getItem() instanceof TalismanPouchItem) {
            return s;
         }
      }

      return ItemStack.EMPTY;
   }

   public static void throwFromPouch(Player player, int slotIndex) {
      if (!player.level().isClientSide()) {
         if (slotIndex >= 0 && slotIndex < 6) {
            ItemStack pouch = findPouch(player);
            if (pouch.isEmpty()) {
               JiangshiDao.LOGGER.info("[PouchThrow] 服务端找不到符袋");
            } else {
               List<ItemStack> slots = getSlots(pouch);
               ItemStack inSlot = slots.get(slotIndex);
               if (!inSlot.isEmpty()) {
                  boolean isSealing = inSlot.is(ModItems.SEALING_TALISMAN.get());
                  boolean isBurning = inSlot.is(ModItems.BURNING_TALISMAN.get());
                  boolean isThunder = inSlot.is(ModItems.THUNDER_TALISMAN.get());
                  boolean isSubjugation = inSlot.is(ModItems.SUBJUGATION_TALISMAN.get());
                  if (isSealing || isBurning || isThunder || isSubjugation) {
                     Item talismanItem = inSlot.getItem();
                     int cooldown = cooldownTicksFor(talismanItem);
                     if (cooldown > 0 && player.getCooldowns().isOnCooldown(inSlot)) {
                        player.sendOverlayMessage(Component.translatable("message.jiangshidao.talisman.cooldown"));
                     } else {
                        ThrowableItemProjectile talisman;
                        if (isSealing) {
                           talisman = new SealingTalismanEntity(player.level(), player);
                        } else if (isBurning) {
                           talisman = new BurningTalismanEntity(player.level(), player);
                        } else if (isSubjugation) {
                           talisman = new SubjugationTalismanEntity(player.level(), player);
                        } else {
                           talisman = new ThunderTalismanEntity(player.level(), player);
                        }

                        talisman.shootFromRotation(player, player.getXRot() - 3.0F, player.getYRot(), 0.0F, 1.5F, 0.4F);
                        player.level().addFreshEntity(talisman);
                        player.level()
                           .playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.8F);
                        inSlot.shrink(1);
                        if (inSlot.isEmpty()) {
                           slots.set(slotIndex, ItemStack.EMPTY);
                        }

                        setSlots(pouch, slots);
                        if (cooldown > 0) {
                           player.getCooldowns().addCooldown(inSlot, cooldown);
                        }

                        syncToClient(player, slots);
                     }
                  }
               }
            }
         }
      }
   }

   private static void syncToClient(Player player, List<ItemStack> slots) {
      if (player instanceof ServerPlayer sp) {
         ServerPlayNetworking.send(sp, new PouchSyncPayload(slots));
      }

      if (player.containerMenu instanceof InventoryMenu menu) {
         menu.broadcastFullState();
      }

      player.getInventory().setChanged();
   }

   public static int cooldownTicksFor(Item item) {
      if (item instanceof BurningTalismanItem) {
         return 200;
      } else if (item instanceof SubjugationTalismanItem) {
         return 60;
      } else {
         return item instanceof ThunderTalismanItem ? 200 : 0;
      }
   }

   public static int slotCount() {
      return 6;
   }

   public static ItemStack applyClick(ItemStack pouch, int index, int button, ItemStack carried) {
      if (index >= 0 && index < 6) {
         List<ItemStack> slots = getSlots(pouch);
         ItemStack inSlot = slots.get(index);
         if (carried.isEmpty()) {
            if (inSlot.isEmpty()) {
               return carried;
            }

            slots.set(index, ItemStack.EMPTY);
            setSlots(pouch, slots);
            return inSlot.copy();
         } else if (inSlot.isEmpty()) {
            if (!isValidContent(carried)) {
               return carried;
            }

            ItemStack rest;
            if (button == 1) {
               slots.set(index, carried.copyWithCount(1));
               rest = carried.copy();
               rest.shrink(1);
            } else {
               slots.set(index, carried.copy());
               rest = ItemStack.EMPTY;
            }

            setSlots(pouch, slots);
            return rest.isEmpty() ? ItemStack.EMPTY : rest;
         } else if (ItemStack.isSameItemSameComponents(carried, inSlot)) {
            int space = inSlot.getMaxStackSize() - inSlot.getCount();
            if (space <= 0) {
               return carried;
            }

            int move = Math.min(space, carried.getCount());
            inSlot.grow(move);
            ItemStack rest = carried.copy();
            rest.shrink(move);
            setSlots(pouch, slots);
            return rest.isEmpty() ? ItemStack.EMPTY : rest;
         } else {
            if (!isValidContent(carried)) {
               return carried;
            }

            slots.set(index, carried.copy());
            setSlots(pouch, slots);
            return inSlot.copy();
         }
      } else {
         return carried;
      }
   }

   public static void applySubmittedContents(Player player, List<ItemStack> submitted, ItemStack cursor) {
      if (!player.level().isClientSide()) {
         ItemStack pouch = findPouch(player);
         if (!pouch.isEmpty()) {
            List<ItemStack> slots = new ArrayList<>(6);

            for (int i = 0; i < 6; i++) {
               ItemStack s = i < submitted.size() && submitted.get(i) != null ? submitted.get(i) : ItemStack.EMPTY;
               if (!s.isEmpty() && !isValidContent(s)) {
                  s = ItemStack.EMPTY;
               }

               if (!s.isEmpty() && s.getCount() > s.getMaxStackSize()) {
                  s = s.copyWithCount(s.getMaxStackSize());
               }

               slots.add(s);
            }

            setSlots(pouch, slots);

            // 关键：符袋内容与鼠标持物必须一起落盘。
            // 之前只写内容物、丢弃 cursor，客户端已把手上物品放进符袋并清空鼠标，
            // 服务端却仍认为鼠标持有该物品，容器状态回同步后物品就"回到"背包，形成复制。
            applySubmittedCursor(player, cursor);

            player.getInventory().setChanged();
         }
      }
   }

   /**
    * 按客户端提交的鼠标持物状态修正服务端容器，防止符袋与背包同时持有同一件物品。
    *
    * @param cursor 客户端结算后的鼠标持物（通常为空，或部分转移后的余量）
    */
   private static void applySubmittedCursor(Player player, ItemStack cursor) {
      ItemStack sanitized = cursor == null ? ItemStack.EMPTY : cursor;
      if (!sanitized.isEmpty()) {
         int max = Math.max(1, sanitized.getMaxStackSize());
         if (sanitized.getCount() > max) {
            sanitized = sanitized.copyWithCount(max);
         }
      }

      if (player.containerMenu != null) {
         ItemStack current = player.containerMenu.getCarried();
         if (!ItemStack.matches(current, sanitized)) {
            player.containerMenu.setCarried(sanitized);
         }
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, display, tooltip, flag);
      int used = usedSlots(stack);
      tooltip.accept(Component.translatable("tooltip.jiangshidao.talisman_pouch.slots", new Object[]{used, 6}).withStyle(ChatFormatting.GOLD));
      tooltip.accept(Component.translatable("tooltip.jiangshidao.talisman_pouch.usage").withStyle(ChatFormatting.GRAY));
      tooltip.accept(Component.translatable("tooltip.jiangshidao.talisman_pouch.keys").withStyle(ChatFormatting.DARK_AQUA));

      for (ItemStack s : getSlots(stack)) {
         if (!s.isEmpty()) {
            tooltip.accept(
               Component.literal("  · ").append(s.getHoverName()).append(Component.literal(" x" + s.getCount())).withStyle(ChatFormatting.DARK_GRAY)
            );
         }
      }
   }
}
