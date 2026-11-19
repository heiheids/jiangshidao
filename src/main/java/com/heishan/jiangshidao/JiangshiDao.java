package com.heishan.jiangshidao;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/**
 * 僵尸道（Jiangshi Dao）——共用常量与日志入口。
 *
 * <p>作者：黑山大叔
 * <br>主页：https://space.bilibili.com/19428259
 * <br>源码：https://github.com/heiheids
 */
public final class JiangshiDao {
   /** 模组命名空间，所有 Identifier 与资源路径都由此派生，禁止各处硬编码字面量。 */
   public static final String MOD_ID = "jiangshidao";
   public static final Logger LOGGER = LogUtils.getLogger();
   /** 诊断日志总开关：排查工人/铜铃问题时打开，平时关闭以免刷屏。 */
   public static final boolean DIAG = true;

   private JiangshiDao() {
   }

   /**
    * 按本模组命名空间构造资源标识符。
    *
    * @param path 资源路径，如 {@code "peach_log"}
    */
   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MOD_ID, path);
   }
}
