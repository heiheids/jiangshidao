package com.heishan.jiangshidao.worker;

import com.heishan.jiangshidao.menu.WorkerMenu;
import com.heishan.jiangshidao.menu.WorkerMenuData;
import com.heishan.jiangshidao.network.ModNetwork;
import com.heishan.jiangshidao.network.WorkerPayloads;
import com.heishan.jiangshidao.registry.ModEffects;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * WorkerMenuOpener。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class WorkerMenuOpener {
   private WorkerMenuOpener() {
   }

   public static void open(ServerPlayer player, int entityId) {
      if (player.level().getEntity(entityId) instanceof LivingEntity worker) {
         if (ModEffects.isSubdued(worker)) {
            if (!(worker.distanceToSqr(player) > 64.0)) {
               final int id = worker.getId();
               TalentSet.ensureAssigned(worker);
               final WorkerMenuData data = new WorkerMenuData(id, TalentSet.describe(worker));
               player.openMenu(new ExtendedMenuProvider<WorkerMenuData>() {
                  public WorkerMenuData getScreenOpeningData(ServerPlayer p) {
                     return data;
                  }

                  public WorkerMenu createMenu(int containerId, Inventory inventory, Player p) {
                     return new WorkerMenu(containerId, inventory, id, data.talents());
                  }

                  public Component getDisplayName() {
                     return Component.translatable("screen.jiangshidao.worker");
                  }
               });
               ModNetwork.sendTo(player, new WorkerPayloads.State(worker.getId(), WorkerData.jobOf(worker).id()));
               ModNetwork.sendTo(player, new WorkerPayloads.CombatState(worker.getId(), WorkerData.followEnabled(worker), WorkerData.attackEnabled(worker)));
            }
         }
      }
   }

   public static boolean canOpen(Player player, LivingEntity target) {
      return ModEffects.isSubdued(target) && target.distanceToSqr(player) <= 64.0;
   }
}
