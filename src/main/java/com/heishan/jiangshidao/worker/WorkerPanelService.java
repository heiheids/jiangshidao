package com.heishan.jiangshidao.worker;

import com.heishan.jiangshidao.accessory.Accessories;
import com.heishan.jiangshidao.accessory.AccessoryInventory;
import com.heishan.jiangshidao.accessory.AccessorySlot;
import com.heishan.jiangshidao.accessory.AccessoryStorage;
import com.heishan.jiangshidao.block.BronzeBellBlockEntity;
import com.heishan.jiangshidao.registry.ModBlocks;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;
import com.heishan.jiangshidao.item.BronzeBellItem;
import com.heishan.jiangshidao.network.ModNetwork;
import com.heishan.jiangshidao.network.WorkerPayloads;
import com.heishan.jiangshidao.registry.ModDataComponents;
import com.heishan.jiangshidao.registry.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * WorkerPanelService。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class WorkerPanelService {
   private static final double MAX_DISTANCE = 8.0;

   private WorkerPanelService() {
   }

   public static void sendPanel(ServerPlayer player, WorkerPayloads.RequestPanel request) {
      if (request.mode() == 0) {
         LivingEntity target = resolveWorker(player, request.entityId());
         if (target != null) {
            ModNetwork.sendTo(
               player,
               new WorkerPayloads.ShowPanel(
                  0,
                  target.getId(),
                  WorkerData.jobOf(target).id(),
                  WorkerData.nameOf(target).isEmpty() ? WorkerData.ensureName(target) : WorkerData.nameOf(target),
                  Math.round(target.getHealth()) + "/" + Math.round(target.getMaxHealth()),
                  talentData(target)
               )
            );
         }
      } else {
         ItemStack bell = player.getMainHandItem();
         if (bell.getItem() instanceof BronzeBellItem && BronzeBellItem.hasMonster(bell)) {
            CompoundTag content = BronzeBellItem.content(bell);
            ModNetwork.sendTo(
               player,
               new WorkerPayloads.ShowPanel(
                  1,
                  0,
                  WorkerData.jobOf(bell).id(),
                  content.getStringOr("BellName", ""),
                  Math.round(content.getFloatOr("BellHealth", 0.0F)) + "/" + Math.round(content.getFloatOr("BellMaxHealth", 0.0F)),
                  content.getCompoundOrEmpty("BellTalents")
               )
            );
         }
      }
   }

   public static void setName(ServerPlayer player, WorkerPayloads.SetName request) {
      String name = request.name() == null ? "" : request.name().trim();
      if (!name.isEmpty() && name.length() <= 16) {
         if (request.mode() == 0) {
            LivingEntity target = resolveWorker(player, request.entityId());
            if (target == null) {
               return;
            }

            WorkerData.setName(target, name);
         } else {
            ItemStack bell = player.getMainHandItem();
            if (!(bell.getItem() instanceof BronzeBellItem) || !BronzeBellItem.hasMonster(bell)) {
               return;
            }

            CompoundTag tag = BronzeBellItem.content(bell).copy();
            tag.putString("BellName", name);
            bell.set(ModDataComponents.BELL_CONTENT.get(), tag);
         }

         player.sendOverlayMessage(Component.translatable("message.jiangshidao.worker.renamed", new Object[]{name}));
      }
   }

   public static void setJob(ServerPlayer player, WorkerPayloads.SetJob request) {
      WorkerJob job = WorkerJob.byId(request.job());
      if (job.implemented()) {
         if (request.mode() == 0) {
            LivingEntity target = resolveWorker(player, request.entityId());
            if (target != null) {
               WorkerData.setJob(target, job);
               player.sendOverlayMessage(Component.translatable("message.jiangshidao.worker.job_set", new Object[]{job.displayName()}));
            }
         } else {
            ItemStack bell = player.getMainHandItem();
            if (bell.getItem() instanceof BronzeBellItem && BronzeBellItem.hasMonster(bell)) {
               WorkerData.setJob(bell, job);
            } else {
               // 铃已放下成方块，物品被消耗，任务无处可写。
               // 此时把任务写进铜铃方块实体，并立即应用到它绑定的那只怪。
               setJobOnPlacedBell(player, job);
            }
         }
      }
   }

   /**
    * 给"已放下成方块"的铜铃分配任务：写入方块实体并同步到绑定怪物。
    *
    * <p>先取准星指向的铜铃；够不着时退回身边最近的已绑定铜铃。
    */
   private static void setJobOnPlacedBell(ServerPlayer player, WorkerJob job) {
      BronzeBellBlockEntity bell = findPlacedBell(player);
      if (bell == null) {
         return;
      }

      bell.setJobId(job.id());
      if (bell.monsterId() != null) {
         LivingEntity bound = findEntity((ServerLevel)player.level(), bell.monsterId());
         if (bound != null) {
            WorkerData.setJob(bound, job);
         }
      }

      player.sendOverlayMessage(Component.translatable("message.jiangshidao.worker.job_set", new Object[]{job.displayName()}));
   }

   private static BronzeBellBlockEntity findPlacedBell(ServerPlayer player) {
      // 优先用视线射线（玩家通常正对着铜铃打开面板）
      var hit = player.pick(8.0, 0.0F, false);
      if (hit instanceof BlockHitResult blockHit
         && player.level().getBlockState(blockHit.getBlockPos()).is(ModBlocks.BRONZE_BELL.get())) {
         BronzeBellBlockEntity bell = BronzeBellBlockEntity.at(player.level(), blockHit.getBlockPos());
         if (bell != null) {
            return bell;
         }
      }

      // 退回：身边 8 格内最近的已绑定铜铃
      BlockPos origin = player.blockPosition();
      BlockPos bestPos = null;
      double bestDist = Double.MAX_VALUE;
      int r = 8;
      for (BlockPos p : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
         if (!player.level().getBlockState(p).is(ModBlocks.BRONZE_BELL.get())) {
            continue;
         }

         BronzeBellBlockEntity bell = BronzeBellBlockEntity.at(player.level(), p);
         if (bell == null || !bell.isBound()) {
            continue;
         }

         double d = p.distSqr(origin);
         if (d < bestDist) {
            bestDist = d;
            bestPos = p.immutable();
         }
      }

      return bestPos == null ? null : BronzeBellBlockEntity.at(player.level(), bestPos);
   }

   private static LivingEntity findEntity(ServerLevel level, UUID id) {
      var entity = level.getEntity(id);
      return entity instanceof LivingEntity living ? living : null;
   }

   private static CompoundTag talentData(LivingEntity worker) {
      TalentSet.ensureAssigned(worker);
      return TalentSet.describe(worker);
   }

   private static LivingEntity resolveWorker(ServerPlayer player, int entityId) {
      if (player.level().getEntity(entityId) instanceof LivingEntity living) {
         if (!ModEffects.isSubdued(living)) {
            return null;
         } else {
            return living.distanceToSqr(player) > 64.0 ? null : living;
         }
      } else {
         return null;
      }
   }

   public static boolean isWorker(Entity entity) {
      return entity instanceof LivingEntity living && ModEffects.isSubdued(living);
   }

   public static AABB workArea(LivingEntity worker, int radius) {
      return worker.getBoundingBox().inflate(radius);
   }

   public static void setCombat(ServerPlayer player, WorkerPayloads.SetCombat payload) {
      LivingEntity worker = resolveWorker(player, payload.entityId());
      if (worker != null) {
         WorkerData.setFollow(worker, payload.follow());
         WorkerData.setAttack(worker, payload.attack());
      }
   }

   public static void setBellMode(ServerPlayer player, int mode) {
      AccessoryStorage storage = AccessoryStorage.get(player.level().getServer());
      if (storage != null) {
         AccessoryInventory inventory = storage.get(player.getUUID());
         ItemStack bell = inventory.get(AccessorySlot.LEGS);
         if (bell.getItem() instanceof BronzeBellItem) {
            bell.set(ModDataComponents.BELL_MODE.get(), BellMode.byCode(mode).code());
            Accessories.sync(player);
         }
      }
   }

   public static BellMode bellModeOf(ServerPlayer player) {
      AccessoryStorage storage = AccessoryStorage.get(player.level().getServer());
      if (storage == null) {
         return BellMode.AGGRESSIVE;
      }

      ItemStack bell = ItemStack.EMPTY;
      AccessoryInventory inventory = storage.peek(player.getUUID());
      if (inventory != null) {
         bell = inventory.get(AccessorySlot.LEGS);
      }

      return !(bell.getItem() instanceof BronzeBellItem)
         ? BellMode.AGGRESSIVE
         : BellMode.byCode((Integer)bell.getOrDefault(ModDataComponents.BELL_MODE.get(), 1));
   }
}
