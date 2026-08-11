# Neo Voxy · 多版本

[English](README_EN.md)

Neo Voxy 由 **JohnSnow** 维护，在 [NHblock714/voxy](https://github.com/NHblock714/voxy) 的基础上继续维护多版本、客户端优化与可选模组兼容。

> [!IMPORTANT]
> 更新 Neo Voxy 后若出现异常，请先删除 Neo Voxy 配置文件和对应存档中的 Voxy 缓存，再重新进入世界。

> [!WARNING]
> Minecraft 1.20.1 的 Oculus 光影支持仍处于实验阶段。当前原生 Forge 移植不依赖 Connector 或 Fabric API，但在完成实机稳定性验证前更推荐无光影使用。

## 支持版本

| 版本 | 安装位置 | 渲染依赖 | Java | 发布文件 |
|---|---|---|---:|---|
| 1.21.1 NeoForge 联动版 | 客户端与服务端 | Sodium 0.8 / Iris | 21 | `neo-voxy-0.3.3-mc1.21.1-neoforge-integrations.jar` |
| 1.21.1 NeoForge 纯客户端版 | 仅客户端 | Sodium 0.8 / Iris | 21 | `neo-voxy-0.2.18-beta-mc1.21.1-neoforge-client.jar` |
| 1.20.1 Forge 纯客户端版 | 仅客户端 | Embeddium / Oculus | 17 | `neo-voxy-0.3.3-1.20.1-alpha.1-forge-client.jar` |
| 26.1.2 NeoForge 纯客户端版 | 仅客户端 | Sodium 0.9 / Iris | 25 | `neo-voxy-0.2.18-beta-mc26.1.2-neoforge-client.jar` |

最终发布 JAR 会剔除不需要的平台原生库、重复模块描述符和构建中间文件；运行所需的着色器、语言、模型与存储依赖不会删除。

## 功能对比

| 功能 | 1.21.1 联动版 | 1.21.1 客户端版 | 1.20.1 客户端版 | 26.1.2 客户端版 |
|---|:---:|:---:|:---:|:---:|
| 地形 LOD、多级细节与持久缓存 | ✅ | ✅ | ✅ | ✅ |
| Sodium / Embeddium 设置界面集成 | ✅ | ✅ | ✅ | ✅ |
| Iris / Oculus 光影管线 | ✅ | ✅ | ⚠️ 实验性 | ✅ |
| 环境雾、天空雾与流体修复 | ✅ | ✅ | ✅ | ✅ |
| 圆形 LOD 淡入 | ✅ | ✅ | — | ✅ |
| 地面植物交叉模型 | ✅ | ✅ | ✅ | ✅ |
| 树叶 LOD 模式 | ✅ | ✅ | ✅ | ✅ |
| 扩展区块请求 | ✅ | ✅ | — | — |
| LOD 构建压力控制 | ✅ | ✅ | ✅ | ✅ |
| 世界曲率 | ✅ | ✅ | — | — |
| 远距离信标光束 | ✅ | — | — | — |
| 远距离玩家、乘骑物与动画 | ✅ | — | — | — |
| Create、Sable、节气与 Domum 联动 | ✅ | — | — | — |

`—` 表示该版本未包含专项功能，不代表基础地形 LOD 一定与该模组冲突。

### 主要可调功能

- 地面植物与树叶：四个版本都提供居中的交叉植物 LOD，以及性能、平衡、质量三种树叶模式。平衡模式剔除隐藏内部面并保留不规则镂空；质量模式保留更多透明细节。
- 扩展区块请求：让 Voxy 主动请求原版距离外的区块。高距离会显著增加 CPU、内存、网络、世界生成与存档负载，联动版上限为 48 区块且默认关闭。
- LOD 构建压力：可在“最高帧数”到“最高追赶”之间调节每帧节点处理与模型烘焙预算。
- 圆形 LOD 淡入：在原版区块与 LOD 之间进行圆形交接。若光影自身已有 LOD 过渡（例如 Photon），应关闭此功能，避免双重过渡、噪点或阴影边界。
- 雾气与云：支持环境雾、天空雾距离、雾强度/密度及自适应云距离；具体选项因版本而异。
- 细分尺寸：控制屏幕空间触发更细 LOD 的阈值；数值越小画质越高，构建和渲染开销也越高。

## 模组兼容性

| 模组或组件 | 支持版本 | 说明 |
|---|---|---|
| Sodium / Iris | 1.21.1、26.1.2 | 必须使用对应 Minecraft 版本支持的 Sodium/Iris 大版本 |
| Embeddium | 1.20.1 | 必需；原生 Forge 渲染后端 |
| Oculus | 1.20.1 | 可选；当前仍为实验性支持 |
| Create | 1.21.1 联动版 | 远景列车、轨道、动态结构与动力部件 |
| Sable | 1.21.1 联动版 | 远景物理结构及深度兼容 |
| Ecliptic Seasons | 1.21.1 联动版 | 远景季节积雪 |
| Domum Ornamentum | 1.21.1 联动版 | 特殊方块着色与独立 LOD 模型 |
| Photon 等自带 LOD 过渡的光影 | 支持光影的版本 | 关闭 Neo Voxy 的 LOD 淡入，避免重复过渡 |

专项联动仅在对应模组已安装时启用。Create、Sable 与节气兼容来自 **NHblock**。

## 构建

Windows 单独构建：

```powershell
.\scripts\build.ps1 integrations-1.21.1
.\scripts\build.ps1 client-1.21.1
.\scripts\build.ps1 client-1.20.1
.\scripts\build.ps1 client-26.1.2
```

构建全部版本：

```powershell
.\scripts\build-all.ps1
```

脚本优先读取 `JAVA_HOME_17`、`JAVA_HOME_21` 与 `JAVA_HOME_25`。Linux/macOS 可使用 `scripts/build.sh` 和 `scripts/build-all.sh`。最终产物统一复制到 `dist/`；GitHub Actions 会并行构建四个版本并发布一个 `neo-voxy-multiversion` 整合产物。

## 许可证

请查看各版本随附的许可证文件。打包的第三方库保留其原许可证。
