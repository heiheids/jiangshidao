package com.heishan.jiangshidao.client;

import com.heishan.jiangshidao.JiangshiDao;
import com.heishan.jiangshidao.block.BronzeBellBlockEntity;
import com.heishan.jiangshidao.registry.ModBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

/**
 * 注视放置好的铜铃时，在准星下方显示它绑定的怪物。
 *
 * <p>数据来自铜铃的方块实体 {@link BronzeBellBlockEntity}：方块实体的数据会随区块同步到客户端，
 * 因此这里能直接读到服务端记录的绑定关系，不再依赖"就近扫描降服单位"这类猜测
 * （{@code WorkerData} 存在服务端持久化数据里，客户端读不到）。
 *
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class BellHudOverlay {
   private static final Identifier ELEMENT_ID = JiangshiDao.id("bell_status");
   private static final int BACKDROP_COLOR = -1879048192;
   private static boolean reportedError;
   /** 诊断日志节流：同一位置的告警最多每 5 秒一条，避免每帧刷屏。 */
   private static long lastDiagAt;
   private static BlockPos lastDiagPos;

   private BellHudOverlay() {
   }

   public static void register() {
      HudElementRegistry.addLast(ELEMENT_ID, BellHudOverlay::extract);
      JiangshiDao.LOGGER.info("[HUD] 铜铃绑定信息提示已注册");
   }

   private static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
      try {
         render(graphics);
      } catch (Throwable t) {
         if (!reportedError) {
            reportedError = true;
            JiangshiDao.LOGGER.error("[HUD] 铜铃信息渲染失败（后续不再重复记录）", t);
         }
      }
   }

   private static void render(GuiGraphicsExtractor graphics) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) {
         return;
      }

      if (mc.gui != null && mc.gui.screen() != null) {
         return;
      }

      if (mc.hitResult == null || mc.hitResult.getType() != Type.BLOCK) {
         return;
      }

      if (!(mc.hitResult instanceof BlockHitResult blockHit)) {
         return;
      }

      BlockPos pos = blockHit.getBlockPos();
      if (!mc.level.getBlockState(pos).is(ModBlocks.BRONZE_BELL.get())) {
         return;
      }

      BronzeBellBlockEntity bell = BronzeBellBlockEntity.at(mc.level, pos);
      Component line;
      if (bell == null || !bell.isBound()) {
         // 兜底：方块实体没记录时，退回到"就近的降服单位"，至少能显示出来。
         var fallback = findNearestSubdued(mc, pos);
         if (fallback == null) {
            line = Component.translatable("hud.jiangshidao.bell.empty");
         } else {
            line = Component.translatable("hud.jiangshidao.bell.bound",
               fallback.hasCustomName() ? fallback.getCustomName() : Component.translatable("hud.jiangshidao.bell.unnamed"));
         }

         if (diagAllowed(pos)) {
            if (bell == null) {
               JiangshiDao.LOGGER.info("[铜铃诊断] 客户端在 {} 读不到方块实体（方块={}）", pos, mc.level.getBlockState(pos));
            } else {
               JiangshiDao.LOGGER.info("[铜铃诊断] 方块实体未绑定；就近降服单位={}", fallback == null ? "无" : fallback.getName().getString());
            }
         }
      } else {
         String name = bell.monsterName();
         Component shown = name.isEmpty()
            ? Component.translatable("hud.jiangshidao.bell.unnamed")
            : Component.literal(name);
         line = Component.translatable("hud.jiangshidao.bell.bound", shown);
      }

      int width = mc.font.width(line);
      int centerX = graphics.guiWidth() / 2;
      int y = graphics.guiHeight() / 2 + 24;
      int x = centerX - width / 2;
      graphics.fill(x - 3, y - 2, x + width + 3, y + 10, BACKDROP_COLOR);
      graphics.text(mc.font, line, x, y, -1, true);
   }

   /**
    * 诊断日志节流：同一方块位置每 5 秒最多输出一条。
    */
   private static boolean diagAllowed(BlockPos pos) {
      long now = System.currentTimeMillis();
      if (pos.equals(lastDiagPos) && now - lastDiagAt < 5000L) {
         return false;
      }

      lastDiagPos = pos;
      lastDiagAt = now;
      return true;
   }

   /**
    * 兜底查找：铜铃方块实体没有绑定记录时，取附近最近的降服单位。
    * 仅依赖客户端可见信息（降服效果会同步、自定义名会同步）。
    */
   private static net.minecraft.world.entity.LivingEntity findNearestSubdued(Minecraft mc, BlockPos pos) {
      var box = new net.minecraft.world.phys.AABB(pos).inflate(32.0, 24.0, 32.0);
      net.minecraft.world.entity.LivingEntity best = null;
      double bestDist = Double.MAX_VALUE;

      for (var living : mc.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box)) {
         if (!com.heishan.jiangshidao.registry.ModEffects.isSubdued(living)) {
            continue;
         }

         double d = living.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
         if (d < bestDist) {
            bestDist = d;
            best = living;
         }
      }

      return best;
   }
}
