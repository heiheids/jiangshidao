package com.heishan.jiangshidao.registry;

import com.heishan.jiangshidao.compat.Reg;
import com.heishan.jiangshidao.compat.RegistryHelper;
import com.heishan.jiangshidao.menu.AccessoryMenu;
import com.heishan.jiangshidao.menu.WorkerMenu;
import com.heishan.jiangshidao.menu.WorkerMenuData;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/**
 * ModMenus。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class ModMenus {
   public static final Reg<MenuType<AccessoryMenu>> ACCESSORY = RegistryHelper.reg(
      BuiltInRegistries.MENU, "accessories", () -> new MenuType<>(AccessoryMenu::new, FeatureFlags.DEFAULT_FLAGS)
   );
   public static final Reg<MenuType<WorkerMenu>> WORKER = RegistryHelper.reg(
      BuiltInRegistries.MENU,
      "worker",
      () -> new ExtendedMenuType<>((windowId, inv, data) -> new WorkerMenu(windowId, inv, data.workerId(), data.talents()), WorkerMenuData.STREAM_CODEC)
   );

   public static void touch() {
   }

   private ModMenus() {
   }
}
