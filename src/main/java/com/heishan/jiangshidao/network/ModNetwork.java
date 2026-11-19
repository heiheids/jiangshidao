package com.heishan.jiangshidao.network;

import com.heishan.jiangshidao.accessory.Accessories;
import com.heishan.jiangshidao.accessory.AccessoryInventory;
import com.heishan.jiangshidao.accessory.AccessoryStorage;
import com.heishan.jiangshidao.block.TalismanPaperBlockEntity;
import com.heishan.jiangshidao.compat.Net;
import com.heishan.jiangshidao.entity.FlyingSwordEntity;
import com.heishan.jiangshidao.item.QuiverItem;
import com.heishan.jiangshidao.item.TalismanPouchItem;
import com.heishan.jiangshidao.item.WoodSwordItem;
import com.heishan.jiangshidao.menu.AccessoryMenu;
import com.heishan.jiangshidao.registry.ModBlocks;
import com.heishan.jiangshidao.worker.WorkerMenuOpener;
import com.heishan.jiangshidao.worker.WorkerPanelService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

/**
 * ModNetwork。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ModNetwork {
   private ModNetwork() {
   }

   public static void register() {
      Net.c2s(ThrowSwordPayload.TYPE, ThrowSwordPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (stack.getItem() instanceof WoodSwordItem) {
               WoodSwordItem.throwSword(player.level(), player, stack);
            }
         }
      }));
      Net.c2s(ThrowPouchTalismanPayload.TYPE, ThrowPouchTalismanPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            TalismanPouchItem.throwFromPouch(player, payload.slotIndex());
         }
      }));
      Net.c2s(PouchContentsPayload.TYPE, PouchContentsPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            TalismanPouchItem.applySubmittedContents(player, payload.slots(), payload.cursor());
         }
      }));
      Net.c2s(SelectTalismanPayload.TYPE, SelectTalismanPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            ServerLevel level = player.level();
            if (level.isLoaded(payload.pos())) {
               if (level.getBlockState(payload.pos()).is(ModBlocks.TALISMAN_PAPER.get())) {
                  if (!(player.distanceToSqr(payload.pos().getX() + 0.5, payload.pos().getY() + 0.5, payload.pos().getZ() + 0.5) > 64.0)) {
                     if (level.getBlockEntity(payload.pos()) instanceof TalismanPaperBlockEntity be) {
                        be.setSelectedType(payload.talismanType());
                        player.sendOverlayMessage(Component.translatable("message.jiangshidao.paper.selected", new Object[]{payload.talismanType()}));
                     }
                  }
               }
            }
         }
      }));
      Net.s2c(PouchSyncPayload.TYPE, PouchSyncPayload.STREAM_CODEC);
      Net.c2s(
         OpenAccessoryPayload.TYPE,
         OpenAccessoryPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(
            () -> {
               if (context.player() instanceof ServerPlayer player) {
                  player.openMenu(
                     new SimpleMenuProvider((id, inventory, p) -> new AccessoryMenu(id, inventory), Component.translatable("screen.jiangshidao.accessories"))
                  );
               }
            }
         )
      );
      Net.c2s(ThrowQuiverSwordPayload.TYPE, ThrowQuiverSwordPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            if (Accessories.hasQuiver(player)) {
               AccessoryInventory inv = AccessoryStorage.get(player.level().getServer()).get(player.getUUID());
               int slot = payload.slot();
               ItemStack sword = inv.getQuiver(slot);
               if (QuiverItem.acceptsSword(sword)) {
                  ItemStack flying = sword.copy();
                  inv.setQuiver(slot, ItemStack.EMPTY);
                  FlyingSwordEntity flyingSword = WoodSwordItem.spawnFlyingSword(player.level(), player, flying);
                  if (flyingSword != null) {
                     flyingSword.setQuiverSlot(slot);
                  }

                  AccessoryStorage.get(player.level().getServer()).setDirty();
                  Accessories.sync(player);
                  player.containerMenu.broadcastChanges();
               }
            }
         }
      }));
      Net.s2c(AccessorySyncPayload.TYPE, AccessorySyncPayload.STREAM_CODEC);
      Net.c2s(WorkerPayloads.RequestPanel.TYPE, WorkerPayloads.RequestPanel.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            WorkerPanelService.sendPanel(player, payload);
         }
      }));
      Net.c2s(WorkerPayloads.OpenMenu.TYPE, WorkerPayloads.OpenMenu.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            WorkerMenuOpener.open(player, payload.entityId());
         }
      }));
      Net.c2s(WorkerPayloads.SetJob.TYPE, WorkerPayloads.SetJob.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            WorkerPanelService.setJob(player, payload);
         }
      }));
      Net.c2s(WorkerPayloads.SetName.TYPE, WorkerPayloads.SetName.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            WorkerPanelService.setName(player, payload);
         }
      }));
      Net.c2s(
         WorkerPayloads.SetCombat.TYPE,
         WorkerPayloads.SetCombat.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> WorkerPanelService.setCombat(context.player(), payload))
      );
      Net.s2c(WorkerPayloads.CombatState.TYPE, WorkerPayloads.CombatState.STREAM_CODEC);
      Net.c2s(
         WorkerPayloads.SetBellMode.TYPE,
         WorkerPayloads.SetBellMode.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> WorkerPanelService.setBellMode(context.player(), payload.mode()))
      );
      Net.s2c(WorkerPayloads.State.TYPE, WorkerPayloads.State.STREAM_CODEC);
      Net.s2c(WorkerPayloads.ShowPanel.TYPE, WorkerPayloads.ShowPanel.STREAM_CODEC);
   }

   public static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
      ServerPlayNetworking.send(player, payload);
   }

   public static void sendThrow(ServerPlayer player) {
      ServerPlayNetworking.send(player, new ThrowSwordPayload());
   }
}
