package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;

import com.heishan.jiangshidao.entity.GhostWomanVariant;
import com.heishan.jiangshidao.entity.JiangshiType;
import com.heishan.jiangshidao.network.OpenAccessoryPayload;
import com.heishan.jiangshidao.network.WorkerPayloads;
import com.heishan.jiangshidao.registry.ModEntities;
import com.heishan.jiangshidao.registry.ModMenus;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.Identifier;

/**
 * ClientSetup。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ClientSetup {
   public static final KeyMapping ACCESSORY_KEY = new KeyMapping(
      "key.jiangshidao.accessory", 75, Category.register(JiangshiDao.id("main"))
   );

   private ClientSetup() {
   }

   public static void register() {
      ModRenderers.registerModelLayers();
      registerEntityRenderers();
      registerScreens();
      registerKeys();
      registerClientReceivers();
      registerTick();
   }

   private static void registerEntityRenderers() {
      for (JiangshiType type : JiangshiType.values()) {
         EntityRendererRegistry.register(ModEntities.get(type), ctx -> new JiangshiRenderer(ctx, type));
      }

      for (GhostWomanVariant variant : GhostWomanVariant.values()) {
         EntityRendererRegistry.register(ModEntities.ghost(variant), GhostWomanRenderer::new);
      }

      EntityRendererRegistry.register(ModEntities.SEALING_TALISMAN.get(), ThrownItemRenderer::new);
      EntityRendererRegistry.register(ModEntities.BURNING_TALISMAN.get(), ThrownItemRenderer::new);
      EntityRendererRegistry.register(ModEntities.THUNDER_TALISMAN.get(), ThrownItemRenderer::new);
      EntityRendererRegistry.register(ModEntities.SUBJUGATION_TALISMAN.get(), ThrownItemRenderer::new);
      EntityRendererRegistry.register(ModEntities.FLYING_SWORD.get(), FlyingSwordRenderer::new);
   }

   private static void registerScreens() {
      MenuScreens.register(ModMenus.ACCESSORY.get(), AccessoryContainerScreen::new);
      MenuScreens.register(ModMenus.WORKER.get(), WorkerContainerScreen::new);
   }

   private static void registerKeys() {
      KeyMappingHelper.registerKeyMapping(ACCESSORY_KEY);
   }

   private static void registerTick() {
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         while (ACCESSORY_KEY.consumeClick()) {
            ClientPlayNetworking.send(new OpenAccessoryPayload());
         }
      });
   }

   private static void registerClientReceivers() {
      ClientPlayNetworking.registerGlobalReceiver(WorkerPayloads.ShowPanel.TYPE, (payload, context) -> {
         Minecraft client = context.client();
         client.execute(() -> {
            if (payload.mode() == 1) {
               client.setScreenAndShow(new BellScreen(payload.job(), payload.workerName(), payload.healthText(), payload.talents()));
            }
         });
      });
      ClientPlayNetworking.registerGlobalReceiver(
         WorkerPayloads.State.TYPE, (payload, context) -> context.client().execute(() -> ClientWorkerHandler.updateJob(payload.entityId(), payload.job()))
      );
      ClientPlayNetworking.registerGlobalReceiver(
         WorkerPayloads.CombatState.TYPE,
         (payload, context) -> context.client().execute(() -> ClientWorkerHandler.updateCombat(payload.entityId(), payload.follow(), payload.attack()))
      );
   }
}
