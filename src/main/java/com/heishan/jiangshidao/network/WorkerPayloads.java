package com.heishan.jiangshidao.network;

import com.heishan.jiangshidao.JiangshiDao;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

/**
 * WorkerPayloads。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class WorkerPayloads {
   public static final int MODE_ENTITY = 0;
   public static final int MODE_BELL = 1;

   private WorkerPayloads() {
   }

   public record CombatState(int entityId, boolean follow, boolean attack) implements CustomPacketPayload {
      public static final Type<WorkerPayloads.CombatState> TYPE = new Type(JiangshiDao.id("worker_combat_state"));
      public static final StreamCodec<RegistryFriendlyByteBuf, WorkerPayloads.CombatState> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         WorkerPayloads.CombatState::entityId,
         ByteBufCodecs.BOOL,
         WorkerPayloads.CombatState::follow,
         ByteBufCodecs.BOOL,
         WorkerPayloads.CombatState::attack,
         WorkerPayloads.CombatState::new
      );

      public Type<WorkerPayloads.CombatState> type() {
         return TYPE;
      }
   }

   public record OpenMenu(int entityId) implements CustomPacketPayload {
      public static final Type<WorkerPayloads.OpenMenu> TYPE = new Type(JiangshiDao.id("open_worker_menu"));
      public static final StreamCodec<RegistryFriendlyByteBuf, WorkerPayloads.OpenMenu> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT, WorkerPayloads.OpenMenu::entityId, WorkerPayloads.OpenMenu::new
      );

      public Type<WorkerPayloads.OpenMenu> type() {
         return TYPE;
      }
   }

   public record RequestPanel(int mode, int entityId) implements CustomPacketPayload {
      public static final Type<WorkerPayloads.RequestPanel> TYPE = new Type(JiangshiDao.id("request_worker_panel"));
      public static final StreamCodec<RegistryFriendlyByteBuf, WorkerPayloads.RequestPanel> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         WorkerPayloads.RequestPanel::mode,
         ByteBufCodecs.VAR_INT,
         WorkerPayloads.RequestPanel::entityId,
         WorkerPayloads.RequestPanel::new
      );

      public Type<WorkerPayloads.RequestPanel> type() {
         return TYPE;
      }
   }

   public record SetBellMode(int mode) implements CustomPacketPayload {
      public static final Type<WorkerPayloads.SetBellMode> TYPE = new Type(JiangshiDao.id("set_bell_mode"));
      public static final StreamCodec<RegistryFriendlyByteBuf, WorkerPayloads.SetBellMode> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT, WorkerPayloads.SetBellMode::mode, WorkerPayloads.SetBellMode::new
      );

      public Type<WorkerPayloads.SetBellMode> type() {
         return TYPE;
      }
   }

   public record SetCombat(int entityId, boolean follow, boolean attack) implements CustomPacketPayload {
      public static final Type<WorkerPayloads.SetCombat> TYPE = new Type(JiangshiDao.id("worker_set_combat"));
      public static final StreamCodec<RegistryFriendlyByteBuf, WorkerPayloads.SetCombat> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         WorkerPayloads.SetCombat::entityId,
         ByteBufCodecs.BOOL,
         WorkerPayloads.SetCombat::follow,
         ByteBufCodecs.BOOL,
         WorkerPayloads.SetCombat::attack,
         WorkerPayloads.SetCombat::new
      );

      public Type<WorkerPayloads.SetCombat> type() {
         return TYPE;
      }
   }

   public record SetJob(int mode, int entityId, String job) implements CustomPacketPayload {
      public static final Type<WorkerPayloads.SetJob> TYPE = new Type(JiangshiDao.id("set_worker_job"));
      public static final StreamCodec<RegistryFriendlyByteBuf, WorkerPayloads.SetJob> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         WorkerPayloads.SetJob::mode,
         ByteBufCodecs.VAR_INT,
         WorkerPayloads.SetJob::entityId,
         ByteBufCodecs.STRING_UTF8,
         WorkerPayloads.SetJob::job,
         WorkerPayloads.SetJob::new
      );

      public Type<WorkerPayloads.SetJob> type() {
         return TYPE;
      }
   }

   public record SetName(int mode, int entityId, String name) implements CustomPacketPayload {
      public static final Type<WorkerPayloads.SetName> TYPE = new Type(JiangshiDao.id("set_worker_name"));
      public static final StreamCodec<RegistryFriendlyByteBuf, WorkerPayloads.SetName> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         WorkerPayloads.SetName::mode,
         ByteBufCodecs.VAR_INT,
         WorkerPayloads.SetName::entityId,
         ByteBufCodecs.STRING_UTF8,
         WorkerPayloads.SetName::name,
         WorkerPayloads.SetName::new
      );

      public Type<WorkerPayloads.SetName> type() {
         return TYPE;
      }
   }

   public record ShowPanel(int mode, int entityId, String job, String workerName, String healthText, CompoundTag talents) implements CustomPacketPayload {
      public static final Type<WorkerPayloads.ShowPanel> TYPE = new Type(JiangshiDao.id("show_worker_panel"));
      public static final StreamCodec<RegistryFriendlyByteBuf, WorkerPayloads.ShowPanel> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         WorkerPayloads.ShowPanel::mode,
         ByteBufCodecs.VAR_INT,
         WorkerPayloads.ShowPanel::entityId,
         ByteBufCodecs.STRING_UTF8,
         WorkerPayloads.ShowPanel::job,
         ByteBufCodecs.STRING_UTF8,
         WorkerPayloads.ShowPanel::workerName,
         ByteBufCodecs.STRING_UTF8,
         WorkerPayloads.ShowPanel::healthText,
         ByteBufCodecs.COMPOUND_TAG,
         WorkerPayloads.ShowPanel::talents,
         WorkerPayloads.ShowPanel::new
      );

      public Type<WorkerPayloads.ShowPanel> type() {
         return TYPE;
      }
   }

   public record State(int entityId, String job) implements CustomPacketPayload {
      public static final Type<WorkerPayloads.State> TYPE = new Type(JiangshiDao.id("worker_state"));
      public static final StreamCodec<RegistryFriendlyByteBuf, WorkerPayloads.State> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT, WorkerPayloads.State::entityId, ByteBufCodecs.STRING_UTF8, WorkerPayloads.State::job, WorkerPayloads.State::new
      );

      public Type<WorkerPayloads.State> type() {
         return TYPE;
      }
   }
}
