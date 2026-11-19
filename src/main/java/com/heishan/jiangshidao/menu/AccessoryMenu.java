package com.heishan.jiangshidao.menu;

import com.heishan.jiangshidao.accessory.Accessories;
import com.heishan.jiangshidao.accessory.AccessoryInventory;
import com.heishan.jiangshidao.accessory.AccessorySlot;
import com.heishan.jiangshidao.accessory.AccessoryStorage;
import com.heishan.jiangshidao.item.QuiverItem;
import com.heishan.jiangshidao.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * AccessoryMenu —— 容器菜单数据与槽位布局。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class AccessoryMenu extends AbstractContainerMenu {
   public static final int ACCESSORY_COUNT = AccessorySlot.size();
   private static final int PLAYER_SLOTS = 36;
   public static final int[] ACCESSORY_Y = new int[]{20, 42, 64, 86};
   public static final int ACCESSORY_X = 104;
   public static final int QUIVER_X = 26;
   public static final int QUIVER_Y = 20;
   public static final int QUIVER_GAP = 22;
   public static final int INV_Y = 146;
   private final Container accessoryContainer;
   private final Container quiverContainer;
   private final Player owner;

   public ItemStack legsAccessory() {
      return this.accessoryContainer.getItem(AccessorySlot.LEGS.ordinal());
   }

   public AccessoryMenu(int containerId, Inventory playerInventory) {
      super(ModMenus.ACCESSORY.get(), containerId);
      this.owner = playerInventory.player;
      AccessoryInventory data = Accessories.of(this.owner);
      this.accessoryContainer = new AccessoryContainer(data);
      this.quiverContainer = new QuiverContainer(data);
      ((AccessoryContainer)this.accessoryContainer).setChangeListener(() -> {
         this.slotsChanged(this.accessoryContainer);
         Accessories.ejectQuiverSwords(this.owner);
      });
      ((QuiverContainer)this.quiverContainer).setChangeListener(() -> this.slotsChanged(this.quiverContainer));

      for (int i = 0; i < 3; i++) {
         this.addSlot(new AccessoryMenu.QuiverSlot(this.quiverContainer, this.accessoryContainer, i, 26, 20 + i * 22));
      }

      for (int i = 0; i < ACCESSORY_COUNT; i++) {
         this.addSlot(new AccessoryMenu.EquipmentSlot(this.accessoryContainer, i, 104, ACCESSORY_Y[i]));
      }

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 146 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 204));
      }
   }

   public Container accessoryContainer() {
      return this.accessoryContainer;
   }

   public Container quiverContainer() {
      return this.quiverContainer;
   }

   public boolean stillValid(Player player) {
      return true;
   }

   public void slotsChanged(Container container) {
      super.slotsChanged(container);
      if ((container == this.accessoryContainer || container == this.quiverContainer) && this.owner instanceof ServerPlayer serverPlayer) {
         AccessoryStorage.get(serverPlayer.level().getServer()).setDirty();
         Accessories.sync(serverPlayer);
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      Slot slot = (Slot)this.slots.get(index);
      if (!slot.hasItem()) {
         return ItemStack.EMPTY;
      }

      ItemStack inSlot = slot.getItem();
      ItemStack original = inSlot.copy();
      // 槽位注册顺序是：0..2 = 剑囊槽，3..6 = 饰品槽（头/背/腰/腿）。
      // 原先用 ACCESSORY_COUNT(=4) 当分界，实际只覆盖到第一个饰品槽，
      // 其余三个饰品槽与剑囊槽的分界都是错的。这里按真实区间计算。
      int quiverEnd = AccessoryInventory.QUIVER_SLOTS;
      int equipEnd = quiverEnd + ACCESSORY_COUNT;
      int playerEnd = equipEnd + 36;
      if (index < equipEnd) {
         if (!this.moveItemStackTo(inSlot, equipEnd, playerEnd, true)) {
            return ItemStack.EMPTY;
         }
      } else if (!this.moveItemStackTo(inSlot, quiverEnd, equipEnd, false)
         && !this.moveItemStackTo(inSlot, 0, quiverEnd, false)) {
         return ItemStack.EMPTY;
      }

      if (inSlot.isEmpty()) {
         slot.setByPlayer(ItemStack.EMPTY);
      } else {
         slot.setChanged();
      }

      return original;
   }

   private static class EquipmentSlot extends Slot {
      private final AccessorySlot type;

      EquipmentSlot(Container container, int index, int x, int y) {
         super(container, index, x, y);
         this.type = AccessorySlot.byIndex(index);
      }

      public AccessorySlot type() {
         return this.type;
      }

      public boolean mayPlace(ItemStack stack) {
         return this.type.accepts(stack);
      }

      public int getMaxStackSize() {
         return 1;
      }
   }

   private static class QuiverSlot extends Slot {
      private final Container accessoryContainer;

      QuiverSlot(Container quiverContainer, Container accessoryContainer, int index, int x, int y) {
         super(quiverContainer, index, x, y);
         this.accessoryContainer = accessoryContainer;
      }

      public boolean mayPlace(ItemStack stack) {
         ItemStack back = this.accessoryContainer.getItem(AccessorySlot.BACK.ordinal());
         return back.getItem() instanceof QuiverItem && QuiverItem.acceptsSword(stack);
      }

      public int getMaxStackSize() {
         return 1;
      }
   }
}
