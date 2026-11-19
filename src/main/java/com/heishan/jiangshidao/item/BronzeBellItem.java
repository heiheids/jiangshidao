package com.heishan.jiangshidao.item;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.block.BronzeBellBlockEntity;
import com.heishan.jiangshidao.compat.PersistentData;
import com.heishan.jiangshidao.registry.ModBlocks;
import com.heishan.jiangshidao.registry.ModDataComponents;
import com.heishan.jiangshidao.registry.ModEffects;
import com.heishan.jiangshidao.worker.TalentSet;
import com.heishan.jiangshidao.worker.WorkerData;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * BronzeBellItem —— 物品行为（使用、命中、提示等）。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public class BronzeBellItem extends Item {
   public BronzeBellItem(Properties properties) {
      super(properties);
   }

   public static CompoundTag content(ItemStack stack) {
      return (CompoundTag)stack.getOrDefault(ModDataComponents.BELL_CONTENT.get(), new CompoundTag());
   }

   public static CompoundTag pack(LivingEntity entity) {
      CompoundTag tag = new CompoundTag();
      TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
      entity.saveWithoutId(output);
      tag.merge(output.buildResult());
      tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
      String bellWorkerName = WorkerData.nameOf(entity);
      if (bellWorkerName.isEmpty()) {
         bellWorkerName = WorkerData.ensureName(entity);
      }

      tag.putString("BellName", bellWorkerName);
      UUID bellOwner = WorkerData.ownerOf(entity);
      if (bellOwner != null) {
         tag.putString("BellOwner", bellOwner.toString());
      }

      tag.putFloat("Health", entity.getHealth());
      tag.putFloat("BellHealth", entity.getMaxHealth());
      tag.putFloat("BellMaxHealth", entity.getMaxHealth());
      TalentSet.ensureAssigned(entity);
      tag.put("BellTalents", TalentSet.describe(entity));
      tag.putString("BellJob", WorkerData.jobOf(entity).id());
      tag.putBoolean("BellReleased", false);
      return tag;
   }

   public static Entity unpack(CompoundTag tag, Level level) {
      CompoundTag copy = tag.copy();
      copy.remove("BellName");
      copy.remove("BellOwner");
      copy.remove("BellHealth");
      copy.remove("BellMaxHealth");
      copy.remove("BellTalents");
      copy.remove("BellEntityUuid");
      copy.remove("BellReleased");
      copy.remove("BellJob");
      return EntityType.loadEntityRecursive(copy, level, new EntitySpawnRequest(EntitySpawnReason.MOB_SUMMONED, true), e -> e);
   }

   public static boolean hasMonster(ItemStack stack) {
      return !stack.has(ModDataComponents.BELL_CONTENT.get()) ? false : !content(stack).getBooleanOr("BellReleased", false);
   }

   private static BlockPos findFreeSpot(Level level, BlockPos pos) {
      BlockPos spot = pos;

      for (int i = 0; i < 4; i++) {
         if (level.getBlockState(spot).canBeReplaced()) {
            return spot;
         }

         spot = spot.above();
      }

      return pos;
   }

   /**
    * 解绑这只怪原先所属的铜铃（若存在且确实是它）。
    *
    * <p>用于"收起怪物"与"重新放下"两个时机，保证同一只怪最多被一个铜铃认领。
    */
   private static void unbindFormerBell(LivingEntity monster) {
      long raw = PersistentData.of(monster).getLongOr("JiangshiBellPos", 0L);
      if (raw == 0L) {
         return;
      }

      BlockPos former = BlockPos.of(raw);
      BronzeBellBlockEntity bell = BronzeBellBlockEntity.at(monster.level(), former);
      if (bell != null && monster.getUUID().equals(bell.monsterId())) {
         bell.unbind();
      }
   }

   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
      if (player.level().isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      if (hasMonster(stack)) {
         return InteractionResult.PASS;
      }

      if (!ModEffects.isSubdued(target)) {
         return InteractionResult.PASS;
      }

      CompoundTag captured = pack(target);
      captured.putBoolean("BellReleased", false);
      stack.set(ModDataComponents.BELL_CONTENT.get(), captured);

      // 这只怪若原本绑在某个铜铃上（例如先放下、又被收回），要把那个铃解绑，
      // 否则会出现"一只怪被多个铜铃认领"，其中一个铃永远显示着它。
      unbindFormerBell(target);

      target.discard();
      player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 0.8F);
      player.sendOverlayMessage(Component.translatable("message.jiangshidao.bell.captured"));
      return InteractionResult.CONSUME;
   }

   public InteractionResult useOn(UseOnContext context) {
      ItemStack stack = context.getItemInHand();
      Level level = context.getLevel();
      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      }

      boolean placedBell = false;
      BlockPos placedAt = null;
      if (!level.isClientSide()) {
         JiangshiDao.LOGGER.info("[铜铃诊断] useOn 进入：客户端={} 潜行={} 铃内有怪={} 点击位置={} 面={}",
            level.isClientSide(), player.isShiftKeyDown(), hasMonster(stack),
            context.getClickedPos(), context.getClickedFace());
      }

      // 关键：放下时无论是否潜行，都要先把铜铃方块放出来。
      // 方块是绑定锚点——原先只有潜行才放方块，不潜行会把怪物直接放出来而不留铃，
      // 于是"无处记录绑了哪只怪"，HUD 只能显示未绑定。
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      BlockPos placeAt = context.getClickedPos().relative(context.getClickedFace());
      if (!level.getBlockState(placeAt).canBeReplaced()) {
         return InteractionResult.PASS;
      }

      level.setBlockAndUpdate(placeAt, ModBlocks.BRONZE_BELL.get().defaultBlockState());
      level.playSound(null, placeAt.getX() + 0.5, placeAt.getY() + 0.5, placeAt.getZ() + 0.5, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 0.8F, 1.4F);

      if (!hasMonster(stack)) {
         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
         }

         return InteractionResult.CONSUME;
      } else {
         placedBell = true;
         placedAt = placeAt;
         if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
         }

         CompoundTag tag = content(stack);
         Entity entity = unpack(tag, level);
         if (entity == null) {
            player.sendOverlayMessage(Component.translatable("message.jiangshidao.bell.release_failed"));
            return InteractionResult.FAIL;
         }

         // 出生点可以是附近的空位，但「家」必须锚定铜铃方块本身：
         // 之前把 findFreeSpot 的结果当作家，落点一旦偏移就会与铜铃坐标对不上，
         // 表现为"收过怪的铜铃放下后仍显示未绑定"。绑定要一直持续到怪物死亡。
         BlockPos bellPos = placedBell && placedAt != null ? placedAt : null;
         BlockPos spawnPos = placedBell && placedAt != null ? placedAt.above() : context.getClickedPos().relative(context.getClickedFace());
         spawnPos = findFreeSpot(level, spawnPos);
         Vec3 pos = Vec3.atBottomCenterOf(spawnPos);
         if (entity instanceof LivingEntity living) {
            TalentSet.restoreFromDescription(living, tag.getCompoundOrEmpty("BellTalents"));
            if (tag.contains("BellJob")) {
               WorkerData.setJob(living, WorkerData.jobOf(stack));
            }

            WorkerData.setHome(living, bellPos != null ? bellPos : spawnPos);
            living.addEffect(new MobEffectInstance(ModEffects.SUBDUED, -1, 0, false, true, true));
            String ownerRaw = tag.getStringOr("BellOwner", "");
            boolean ownerRestored = false;
            if (!ownerRaw.isEmpty()) {
               try {
                  UUID restoredOwner = UUID.fromString(ownerRaw);
                  WorkerData.setOwner(living, restoredOwner);
                  ownerRestored = true;
               } catch (IllegalArgumentException invalidEntityTag) {
               }
            }

            if (!ownerRestored) {
               WorkerData.setOwner(living, player.getUUID());
            }

            PersistentData.of(living).remove("JiangshiReviveDeadline");
            living.setHealth(living.getMaxHealth());
            if (living instanceof Mob mob) {
               mob.setTarget(null);
               mob.setLastHurtByMob(null);
            }
         }

         entity.snapTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0F);
         if (!level.addFreshEntity(entity)) {
            player.sendOverlayMessage(Component.translatable("message.jiangshidao.bell.release_failed"));
            return InteractionResult.FAIL;
         }

         if (entity instanceof LivingEntity added) {
            added.setHealth(added.getMaxHealth());
            TalentSet.of(added).applyAttributeModifiers();
            PersistentData.flush(added);

            // 把"绑定哪只怪"写进铜铃方块实体：方块实体数据会同步给客户端，
            // HUD 据此显示绑定信息；同时记下铜铃坐标，便于怪物死亡时反向清除。
            if (placedBell && placedAt != null) {
               // 先解除旧绑定，避免同一只怪被两个铜铃同时认领。
               unbindFormerBell(added);

               BronzeBellBlockEntity bell = BronzeBellBlockEntity.at(level, placedAt);
               if (bell != null) {
                  bell.bind(added.getUUID(), WorkerData.nameOf(added));
                  // 铃上若已记录任务（方块形态下分配过），放出时直接沿用。
                  String jobId = bell.jobId();
                  if (!jobId.isEmpty()) {
                     WorkerData.setJob(added, com.heishan.jiangshidao.worker.WorkerJob.byId(jobId));
                  }

                  JiangshiDao.LOGGER.info("[铜铃诊断] 已绑定 monsterId={} name={} 铜铃位置={}",
                     added.getUUID(), WorkerData.nameOf(added), placedAt);
               } else {
                  JiangshiDao.LOGGER.warn("[铜铃诊断] 绑定失败：{} 处没有铜铃方块实体（方块={}）",
                     placedAt, level.getBlockState(placedAt));
               }

               PersistentData.of(added).putLong("JiangshiBellPos", placedAt.asLong());
               PersistentData.flush(added);
            } else {
               JiangshiDao.LOGGER.info("[铜铃诊断] 未写入绑定：placedBell={} placedAt={}", placedBell, placedAt);
            }
         }

         CompoundTag keep = tag.copy();
         keep.putString("BellEntityUuid", entity.getUUID().toString());
         keep.putBoolean("BellReleased", true);
         keep.putLong("BellHome", spawnPos.asLong());
         keep.putString("BellDim", level.dimension().identifier().toString());
         if (entity instanceof LivingEntity keptLiving) {
            keep.putFloat("BellHealth", keptLiving.getHealth());
            keep.putFloat("BellMaxHealth", keptLiving.getMaxHealth());
         }

         stack.set(ModDataComponents.BELL_CONTENT.get(), keep);
         if (placedBell && !player.getAbilities().instabuild) {
            stack.shrink(1);
         }

         level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
         player.sendOverlayMessage(Component.translatable("message.jiangshidao.bell.released"));
         return InteractionResult.CONSUME;
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, display, tooltip, flag);
      CompoundTag tag = content(stack);
      String name = tag.getStringOr("BellName", "");
      if (!tag.isEmpty() && !name.isEmpty()) {
         float health = tag.getFloatOr("BellHealth", 0.0F);
         float maxHealth = tag.getFloatOr("BellMaxHealth", 0.0F);
         boolean released = tag.getBooleanOr("BellReleased", false);
         tooltip.accept(Component.translatable("tooltip.jiangshidao.bronze_bell.contains", new Object[]{name}).withStyle(ChatFormatting.GOLD));
         tooltip.accept(
            Component.translatable("tooltip.jiangshidao.bronze_bell.health", new Object[]{String.format("%.1f", health), String.format("%.1f", maxHealth)})
               .withStyle(ChatFormatting.RED)
         );
         tooltip.accept(
            Component.translatable(released ? "message.jiangshidao.bell.released" : "tooltip.jiangshidao.bronze_bell.release").withStyle(ChatFormatting.GRAY)
         );
         tooltip.accept(
            Component.translatable("tooltip.jiangshidao.bronze_bell.job", new Object[]{WorkerData.jobOf(stack).displayName()}).withStyle(ChatFormatting.AQUA)
         );
      } else {
         tooltip.accept(
            Component.translatable("tooltip.jiangshidao.bronze_bell.empty").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC})
         );
      }
   }
}
