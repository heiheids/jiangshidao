package com.heishan.jiangshidao.compat;

import com.heishan.jiangshidao.JiangshiDao;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

/**
 * RegistryHelper —— 注册相关。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class RegistryHelper {
   private RegistryHelper() {
   }

   public static <T> Reg<T> reg(Registry<?> registry, String name, Supplier<? extends T> supplier) {
      Registry<T> typed = (Registry<T>)registry;
      return new Reg<>((T)Registry.register(typed, JiangshiDao.id(name), supplier.get()));
   }

   public static <T> Reg<T> reg(Registry<?> registry, Identifier id, Supplier<? extends T> supplier) {
      Registry<T> typed = (Registry<T>)registry;
      return new Reg<>((T)Registry.register(typed, id, supplier.get()));
   }

   public static <T> Holder<T> holder(Registry<T> registry, String name, Supplier<T> supplier) {
      T value = (T)Registry.register(registry, JiangshiDao.id(name), supplier.get());
      return registry.wrapAsHolder(value);
   }
}
