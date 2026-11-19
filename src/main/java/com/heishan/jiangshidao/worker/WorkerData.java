package com.heishan.jiangshidao.worker;

import com.heishan.jiangshidao.compat.PersistentData;
import com.heishan.jiangshidao.registry.ModDataComponents;
import com.heishan.jiangshidao.registry.ModEffects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * WorkerData —— 数据模型。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class WorkerData {
   private static final String KEY_JOB = "JiangshiWorkerJob";
   private static final String KEY_NAME = "JiangshiWorkerName";
   private static final Set<String> ISSUED_NAMES = ConcurrentHashMap.newKeySet();
   private static final String NAME_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
   private static final String KEY_OWNER = "JiangshiWorkerOwner";
   private static final String KEY_FOLLOW = "JiangshiWorkerFollow";
   private static final String KEY_HOME = "JiangshiWorkerHome";
   private static final String KEY_ATTACK = "JiangshiWorkerAttack";

   public static String nameOf(LivingEntity entity) {
      return PersistentData.of(entity).getStringOr("JiangshiWorkerName", "");
   }

   public static void setName(LivingEntity entity, String name) {
      String clean = name == null ? "" : name.trim();
      if (clean.length() > 16) {
         clean = clean.substring(0, 16);
      }

      if (!clean.isEmpty()) {
         PersistentData.of(entity).putString("JiangshiWorkerName", clean);
         PersistentData.flush(entity);
         entity.setCustomName(Component.literal(clean));
         entity.setCustomNameVisible(true);
      }
   }

   public static String ensureName(LivingEntity entity) {
      String existing = nameOf(entity);
      if (!existing.isEmpty()) {
         return existing;
      }

      Random random = new Random();
      int guard = 0;

      String name;
      do {
         StringBuilder sb = new StringBuilder(6);

         for (int i = 0; i < 6; i++) {
            sb.append("ABCDEFGHJKLMNPQRSTUVWXYZ23456789".charAt(random.nextInt("ABCDEFGHJKLMNPQRSTUVWXYZ23456789".length())));
         }

         name = sb.toString();
      } while (!ISSUED_NAMES.add(name) && ++guard < 64);

      setName(entity, name);
      return name;
   }

   private WorkerData() {
   }

   public static UUID ownerOf(LivingEntity entity) {
      CompoundTag data = PersistentData.of(entity);
      if (!data.contains("JiangshiWorkerOwner")) {
         return null;
      }

      try {
         return UUID.fromString(data.getStringOr("JiangshiWorkerOwner", ""));
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   public static void setOwner(LivingEntity entity, UUID owner) {
      if (owner != null) {
         PersistentData.of(entity).putString("JiangshiWorkerOwner", owner.toString());
         PersistentData.flush(entity);
      }
   }

   public static BlockPos homeOf(LivingEntity entity) {
      CompoundTag data = PersistentData.of(entity);
      if (!data.contains("JiangshiWorkerHome")) {
         return null;
      }

      long packed = data.getLongOr("JiangshiWorkerHome", 0L);
      return BlockPos.of(packed);
   }

   public static void setHome(LivingEntity entity, BlockPos pos) {
      if (pos != null) {
         PersistentData.of(entity).putLong("JiangshiWorkerHome", pos.asLong());
         PersistentData.flush(entity);
      }
   }

   public static boolean followEnabled(LivingEntity entity) {
      CompoundTag data = PersistentData.of(entity);
      return !data.contains("JiangshiWorkerFollow") || data.getBooleanOr("JiangshiWorkerFollow", false);
   }

   public static void setFollow(LivingEntity entity, boolean value) {
      PersistentData.of(entity).putBoolean("JiangshiWorkerFollow", value);
      PersistentData.flush(entity);
   }

   public static boolean attackEnabled(LivingEntity entity) {
      CompoundTag data = PersistentData.of(entity);
      return !data.contains("JiangshiWorkerAttack") || data.getBooleanOr("JiangshiWorkerAttack", false);
   }

   public static void setAttack(LivingEntity entity, boolean value) {
      PersistentData.of(entity).putBoolean("JiangshiWorkerAttack", value);
      PersistentData.flush(entity);
   }

   public static WorkerJob jobOf(LivingEntity entity) {
      CompoundTag data = PersistentData.of(entity);
      return WorkerJob.byId(data.getStringOr("JiangshiWorkerJob", ""));
   }

   public static void setJob(LivingEntity entity, WorkerJob job) {
      PersistentData.of(entity).putString("JiangshiWorkerJob", job.id());
      PersistentData.flush(entity);
   }

   public static boolean isSubduedWorker(Entity entity) {
      return entity instanceof LivingEntity living && ModEffects.isSubdued(living);
   }

   public static WorkerJob jobOf(ItemStack bell) {
      CompoundTag content = (CompoundTag)bell.getOrDefault(ModDataComponents.BELL_CONTENT.get(), new CompoundTag());
      return WorkerJob.byId(content.getStringOr("BellJob", ""));
   }

   public static void setJob(ItemStack bell, WorkerJob job) {
      CompoundTag content = (CompoundTag)bell.getOrDefault(ModDataComponents.BELL_CONTENT.get(), new CompoundTag());
      content.putString("BellJob", job.id());
      bell.set(ModDataComponents.BELL_CONTENT.get(), content);
   }
}
