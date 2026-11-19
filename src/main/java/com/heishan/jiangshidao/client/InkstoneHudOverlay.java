package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.block.InkstoneBlockEntity;
import com.heishan.jiangshidao.registry.ModBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;

/**
 * InkstoneHudOverlay —— HUD 覆盖层绘制。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class InkstoneHudOverlay {
   private static final Identifier ELEMENT_ID = JiangshiDao.id("inkstone_status");
   private static final int BACKDROP_COLOR = -1879048192;
   private static boolean reportedError;

   private InkstoneHudOverlay() {
   }

   public static void register() {
      HudElementRegistry.addLast(ELEMENT_ID, InkstoneHudOverlay::extract);
      JiangshiDao.LOGGER.info("[HUD] 砚台状态提示已注册");
   }

   private static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
      try {
         render(graphics);
      } catch (Throwable t) {
         if (!reportedError) {
            reportedError = true;
            JiangshiDao.LOGGER.error("[HUD] 砚台提示渲染失败（后续不再重复记录）", t);
         }
      }
   }

   private static void render(GuiGraphicsExtractor graphics) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         if (mc.gui == null || mc.gui.screen() == null) {
            HitResult hit = mc.hitResult;
            if (hit != null && hit.getType() == Type.BLOCK) {
               if (hit instanceof BlockHitResult blockHit) {
                  BlockPos pos = blockHit.getBlockPos();
                  if (mc.level.getBlockState(pos).is(ModBlocks.INKSTONE.get())) {
                     InkstoneBlockEntity ink = InkstoneBlockEntity.at(mc.level, pos);
                     if (ink != null) {
                        Component line = Component.literal(ink.statusText());
                        int width = mc.font.width(line);
                        int centerX = graphics.guiWidth() / 2;
                        int y = graphics.guiHeight() / 2 + 24;
                        int x = centerX - width / 2;
                        graphics.fill(x - 3, y - 2, x + width + 3, y + 10, -1879048192);
                        graphics.text(mc.font, line, x, y, -1, true);
                     }
                  }
               }
            }
         }
      }
   }
}
