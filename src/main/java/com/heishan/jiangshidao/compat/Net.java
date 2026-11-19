package com.heishan.jiangshidao.compat;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Net。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class Net {
   private Net() {
   }

   public static <T extends CustomPacketPayload> void c2s(Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Net.Handler<T> handler) {
      PayloadTypeRegistry.serverboundPlay().register(type, codec);
      ServerPlayNetworking.registerGlobalReceiver(
         type, (payload, context) -> context.server().execute(() -> handler.handle((T)payload, new Net.Context(context.player(), context.server())))
      );
   }

   public static <T extends CustomPacketPayload> void s2c(Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
      PayloadTypeRegistry.clientboundPlay().register(type, codec);
   }

   public static <T extends CustomPacketPayload> void s2c(Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Net.Handler<T> ignored) {
      s2c(type, codec);
   }

   public record Context(ServerPlayer player, MinecraftServer server) {
      public void enqueueWork(Runnable runnable) {
         this.server.execute(runnable);
      }
   }

   @FunctionalInterface
   public interface Handler<T extends CustomPacketPayload> {
      void handle(T payload, Net.Context context);
   }
}
