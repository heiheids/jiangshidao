# 僵尸道 Jiangshi Dao

Minecraft Fabric 模组 —— 东方僵尸题材的怪物与符箓体系。

- **作者**：黑山大叔
- **主页**：https://space.bilibili.com/19428259
- **源码**：https://github.com/heiheids/jiangshidao
- **目标平台**：Minecraft 26.3 / Fabric
- **当前版本**：2.1.0
- **许可证**：GNU General Public License v3.0（见 [LICENSE](LICENSE)）

---

## 一、本工程的来历（重要）

本工程源码**并非原始手写源码**，而是由硬盘损坏后仅存的成品模组
`mod/jiangshidao26.3-Fabric-2.0.0-fabric.jar` **反编译恢复**而来。

- 恢复工具：Vineflower 1.12.0（仅构建期使用，不是运行时依赖）
- 恢复结果：167 个 class → 137 个 .java，资源 199 个原样解包
- 已通过编译：`gradlew build` 可通过，产物 class 数与资源数与原成品逐一对应

因此代码中存在反编译体特征：注释已丢失、部分局部变量名为 `varN`、
内部类被内联、显式泛型实参与 `(Object)` 中间强制转换被折叠。
这些已在编译修复过程中逐处还原，后续改动请顺手补充注释。

---

## 二、构建

### 环境要求

| 项目 | 要求 | 说明 |
|---|---|---|
| JDK | **25** | 26.3 的 `javaVersion` 为 25，编译与运行都需要 |
| Gradle | 9.7.1 | 已附带 wrapper |

### 命令

```powershell
# 构建（产物在 build/libs/）
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot'
.\gradlew.bat build
```

### 两个必须知道的构建细节

1. **代理**：本机 `~/.gradle/gradle.properties` 里配置了 `127.0.0.1:10808` 代理。
   若 v2rayN 未运行，Gradle 全部下载失败。绕过方式：
   ```powershell
   .\gradlew.bat build "-Dhttp.nonProxyHosts=*" "-Dhttps.nonProxyHosts=*"
   ```
2. **26.3 不再混淆**：官方版本清单只提供 `client` / `server`，没有 mappings 文件，
   Fabric 也不再为 26.3 发布 intermediary。因此本工程用**恒等映射**
   （`mappings/identity.tiny`，official 与 named 同名）让 Loom 正常工作，
   源码中的 `net.minecraft.*` 官方类名可直接对应。
   因为没有 intermediary 命名空间，`remapSourcesJar` 已禁用。

---

## 三、目录结构

```
jiangshidao/
├─ src/main/java/com/heishan/jiangshidao/
│  ├─ JiangshiDao.java            共用常量与日志
│  ├─ JiangshiDaoFabric.java      Fabric 主入口
│  ├─ accessory/   (5)            饰品系统
│  ├─ block/      (12)            墨斗、石磨、铜铃、桃木等方块与方块实体
│  ├─ client/     (34)            渲染器、HUD、界面、输入
│  ├─ compat/      (5)            加载器兼容层（Reg / RegistryHelper / Net）
│  ├─ entity/      (9)            僵尸、鬼女、飞剑、符箓实体
│  ├─ event/      (10)            战斗、生成、雷击、天赋等事件
│  ├─ item/       (10)            桃木剑、铜铃、符纸、箭袋等
│  ├─ menu/        (5)            饰品与工人容器菜单
│  ├─ mixin/      (10)            混入实现
│  ├─ network/    (12)            网络包
│  ├─ registry/   (10)            注册入口
│  ├─ sync/        (1)            状态同步
│  └─ worker/     (10)            工人 AI 与天赋
├─ src/main/resources/            199 个资源：assets / data / mixins / fabric.mod.json
├─ mappings/identity.tiny         26.3 恒等映射
├─ docs/                          技术/使用/踩坑文档
├─ tools/                         模型预览与贴图生成工具链（可选，独立于模组运行）
├─ gradle/wrapper/                Gradle wrapper
├─ build.gradle                   构建脚本
├─ gradle.properties              版本单一来源
├─ LICENSE                        GPL-3.0
└─ 某某工程备份.txt                开发操作日志（只追加）
```

> 本地开发目录 `备份/`（源码备份 zip）、`预览图/`（模型渲染图）与 `mod/`
> （历史成品 jar）体积较大，已写入 `.gitignore`，不纳入仓库。

---

## 四、开发约定

- **模块化**：禁止单文件海量代码。已知待拆分：`worker/WorkerHandler.java`（33 KB）、
  `entity/GhostWomanEntity.java`（16 KB）、`item/WoodSwordItem.java`（12 KB）。
- **禁止硬编码文案**：所有玩家可见文本走语言文件（`zh_cn` / `en_us`，各约 10 KB）。
- **作者信息**：改动涉及元数据时保留作者、主页、GitHub 三项。
- **临时文件**：只能生成在工程目录内。

---

## 五、许可证

本项目以 **GNU General Public License v3.0** 发布，全文见 [LICENSE](LICENSE)。

```
Copyright (C) 2026 黑山大叔 (heiheids)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
```

这意味着你可以自由使用、修改、再分发本项目，包括商业用途，
但**分发修改版时必须同样以 GPL-3.0 开放源代码**，并保留原始版权声明。

### 关于 Minecraft

本项目是 Minecraft 的第三方模组，**与 Mojang Studios / Microsoft 无关联**。
使用与分发本项目时仍需遵守 [Minecraft 最终用户许可协议](https://www.minecraft.net/eula)
及 [Minecraft 使用准则](https://www.minecraft.net/usage-guidelines)。
GPL-3.0 仅覆盖本仓库中的原创代码与资源，不涉及 Minecraft 本体的任何权利。
