package com.heishan.jiangshidao.menu;

import com.heishan.jiangshidao.registry.ModMenus;
import com.heishan.jiangshidao.worker.WorkerEquipment;
import com.heishan.jiangshidao.worker.WorkerHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * WorkerMenu —— 容器菜单数据与槽位布局。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class WorkerMenu extends AbstractContainerMenu {
   public static final int EQUIP_COUNT = 5;
   public static final int PACK_COUNT = 27;
   private static final int PLAYER_SLOTS = 36;
   public static final int EQUIP_X = 8;
   public static final int EQUIP_Y = 20;
   public static final int EQUIP_GAP = 18;
   public static final int HAND_X = 80;
   public static final int HAND_Y = 20;
   public static final int PACK_X = 8;
   public static final int PACK_Y = 100;
   public static final int INV_Y = 170;
   private final Container equipment;
   private final Container backpack;
   private final int workerId;
   private final Player owner;
   private final CompoundTag talentData;

   public WorkerMenu(int containerId, Inventory playerInventory, int workerId) {
      this(containerId, playerInventory, workerId, null);
   }

   public WorkerMenu(int containerId, Inventory playerInventory, int workerId, CompoundTag talentTag) {
      super(ModMenus.WORKER.get(), containerId);
      this.talentData = talentTag == null ? new CompoundTag() : talentTag;
      this.owner = playerInventory.player;
      this.workerId = workerId;
      LivingEntity worker = playerInventory.player.level().getEntity(workerId) instanceof LivingEntity living ? living : null;
      this.equipment = (Container)(worker != null ? new WorkerEquipment(worker) : new SimpleContainer(5));
      // 背包只允许服务端持有真实容器。
      // 客户端如果也 new 一个 WorkerBackpack，会得到一个读不到同步数据的空容器，
      // 于是"放进去"只是客户端幻影，服务端随即用真实数据覆盖 —— 表现为物品弹回/消失。
      this.backpack = (Container)(worker != null && !playerInventory.player.level().isClientSide()
         ? WorkerHandler.backpackOf(worker, playerInventory.player.level().registryAccess())
         : new SimpleContainer(27));

      for (int i = 0; i < 4; i++) {
         this.addSlot(new WorkerMenu.EquipmentSlot(this.equipment, i, 8, 20 + i * 18));
      }

      this.addSlot(new WorkerMenu.EquipmentSlot(this.equipment, 4, 80, 20));

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(this.backpack, col + row * 9, 8 + col * 18, 100 + row * 18));
         }
      }

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 170 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 228));
      }
   }

   public CompoundTag talentData() {
      return this.talentData;
   }

   public int workerId() {
      return this.workerId;
   }

   public boolean stillValid(Player player) {
      return true;
   }

   public ItemStack quickMoveStack(Player player, int index) {
      Slot slot = (Slot)this.slots.get(index);
      if (!slot.hasItem()) {
         return ItemStack.EMPTY;
      }

      ItemStack inSlot = slot.getItem();
      ItemStack original = inSlot.copy();
      int equipEnd = 5;
      int packEnd = equipEnd + 27;
      int playerEnd = packEnd + 36;
      if (index < packEnd) {
         if (!this.moveItemStackTo(inSlot, packEnd, playerEnd, true)) {
            return ItemStack.EMPTY;
         }
      } else if (!this.moveItemStackTo(inSlot, 0, packEnd, false)) {
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
      EquipmentSlot(Container container, int index, int x, int y) {
         super(container, index, x, y);
      }

      public int getMaxStackSize() {
         return 1;
      }
   }
}
