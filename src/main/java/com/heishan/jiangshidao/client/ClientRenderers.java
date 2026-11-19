package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.registry.ModBlockEntities;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityTypes;

/**
 * ClientRenderers。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ClientRenderers {
   private ClientRenderers() {
   }

   public static void register() {
      BlockEntityRendererRegistry.register(ModBlockEntities.STONE_MILL.get(), StoneMillRenderer::new);
      JiangshiDao.LOGGER.info("[渲染] 石磨方块实体渲染器已注册：{}（上扇绕 Y 轴旋转）", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(ModBlockEntities.STONE_MILL.get()));
      LivingEntityRenderLayerRegistrationCallback.EVENT.register((LivingEntityRenderLayerRegistrationCallback)(entityType, renderer, helper, context) -> {
         if (entityType == EntityTypes.PLAYER) {
            // 回调给出的渲染器是通配型 LivingEntityRenderer，这里按玩家实际类型做一次显式转换。
            RenderLayerParent<AvatarRenderState, PlayerModel> parent =
               (RenderLayerParent<AvatarRenderState, PlayerModel>)(Object)renderer;
            helper.register(new QuiverRenderer(parent));
            helper.register(new HeadwearRenderer(parent));
            JiangshiDao.LOGGER.info("[渲染] 玩家渲染层已挂上剑囊渲染器 QuiverRenderer（背剑）");
            JiangshiDao.LOGGER.info("[渲染] 玩家渲染层已挂上头部饰品渲染器 HeadwearRenderer（纯阳巾）");
         }
      });
   }
}
