package com.heishan.jiangshidao.mixin.client;

import com.heishan.jiangshidao.accessory.AccessoryClientCache;
import com.heishan.jiangshidao.accessory.AccessoryInventory;
import com.heishan.jiangshidao.accessory.AccessorySlot;
import com.heishan.jiangshidao.client.ClientRenderData;
import net.minecraft.client.Minecraft;
import com.heishan.jiangshidao.item.ChunyangJinItem;
import com.heishan.jiangshidao.entity.GhostWomanEntity;
import com.heishan.jiangshidao.client.HeadwearRenderData;
import com.heishan.jiangshidao.client.QuiverRenderData;
import com.heishan.jiangshidao.client.SubduedMarkRenderer;
import com.heishan.jiangshidao.item.QuiverItem;
import com.heishan.jiangshidao.item.WoodSwordItem;
import com.heishan.jiangshidao.registry.ModEffects;
import com.heishan.jiangshidao.sync.SubduedState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
/**
 * LivingEntityRenderStateMixin —— 混入实现，改写原版行为。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public abstract class LivingEntityRenderStateMixin {
   @Shadow
   @Final
   protected ItemModelResolver itemModelResolver;

   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
      at = @At("TAIL")
   )
   private void jiangshidao$extractClientRenderData(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
      FabricRenderState renderState = (FabricRenderState)state;
      boolean subdued = SubduedState.of(entity) || ModEffects.isSubdued(entity);
      renderState.setData(ClientRenderData.SUBDUED, subdued);
      if (entity instanceof Player player) {
         AccessoryInventory inv = AccessoryClientCache.get(player.getUUID());
         ItemStack back = inv.get(AccessorySlot.BACK);
         if (!(back.getItem() instanceof QuiverItem)) {
            renderState.setData(ClientRenderData.QUIVER, null);
         } else {
            QuiverRenderData data = new QuiverRenderData();
            this.itemModelResolver.updateForNonLiving(data.quiver, back, ItemDisplayContext.FIXED, player);

            for (int i = 0; i < 3; i++) {
               ItemStack sword = inv.getQuiver(i);
               if (sword.getItem() instanceof WoodSwordItem) {
                  data.swordStacks[i] = sword;
                  this.itemModelResolver.updateForNonLiving(data.swords[i], sword, ItemDisplayContext.FIXED, player);
               }
            }

            renderState.setData(ClientRenderData.QUIVER, data);
         }

         // 头部饰品（纯阳巾）：解析成渲染数据，交给 HeadwearRenderer 画在头顶
         ItemStack head = inv.get(AccessorySlot.HEAD);
         if (head.getItem() instanceof ChunyangJinItem) {
            HeadwearRenderData headData = new HeadwearRenderData();
            this.itemModelResolver.updateForNonLiving(headData.model, head, ItemDisplayContext.FIXED, player);
            renderState.setData(ClientRenderData.HEADWEAR, headData);
         } else {
            renderState.setData(ClientRenderData.HEADWEAR, null);
         }
      }

      // 纯阳巾能力 1：佩戴已激活道冠的本地玩家，可见 25 格内隐身的女鬼。
      // 只改客户端渲染标志，不动服务端的 setInvisible —— 因此只有佩戴者能看到，
      // 其他玩家客户端不执行本段逻辑，仍然看不见。
      if (entity instanceof GhostWomanEntity ghost) {
         Player local = Minecraft.getInstance().player;
         if (local != null
            && ChunyangJinItem.isWearingActive(local)
            && local.distanceToSqr(ghost) <= ChunyangJinItem.REVEAL_RADIUS_SQR) {
            state.isInvisible = false;
         }
      }
   }

   @Inject(
      method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
      at = @At("TAIL")
   )
   private void jiangshidao$renderSubduedMark(
      LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci
   ) {
      SubduedMarkRenderer.render(state, poseStack, collector, camera);
   }
}
