package com.heishan.jiangshidao.compat;

/**
 * Reg —— 注册相关。
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class Reg<T> {
   private final T value;

   public Reg(T value) {
      this.value = value;
   }

   public T get() {
      return this.value;
   }
}
