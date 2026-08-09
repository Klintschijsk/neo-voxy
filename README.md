# Neo Voxy · Multiversion

[简体中文](#简体中文) · [English](#english)

Neo Voxy is maintained by **JohnSnow** and extends
[NHblock714/voxy](https://github.com/NHblock714/voxy). The `multiversion` branch keeps four supported
editions in one repository while preserving an independent Gradle wrapper and toolchain for each game version.

> [!IMPORTANT]
> 更新后若出现异常，请先删除 Neo Voxy 配置文件和对应存档中的 Voxy 缓存，再重新进入世界。
> If an update causes problems, delete the Neo Voxy configuration and that world's Voxy cache before retrying.

> [!WARNING]
> **Minecraft 1.20.1 的光影支持仍不完善。** Oculus 光影可能出现拖影、天空残影或 LOD 管线不生效；
> 该版本目前更适合无光影使用。

## 简体中文

### 版本

| 版本 | 安装方式 | 渲染依赖 | Java | 发布产物 |
|---|---|---|---:|---|
| 1.21.1 NeoForge 联动版 | 客户端与服务端 | Sodium 0.8 / Iris | 21 | `neo-voxy-0.3.3-mc1.21.1-neoforge-integrations.jar` |
| 1.21.1 NeoForge 纯客户端版 | 仅客户端 | Sodium 0.8 / Iris | 21 | `neo-voxy-0.2.18-beta-mc1.21.1-neoforge-client.jar` |
| 1.20.1 Forge 纯客户端版 | 仅客户端 | Embeddium / Oculus | 17 | `neo-voxy-0.3.3-1.20.1-alpha.1-forge-client.jar` |
| 26.1.2 NeoForge 纯客户端版 | 仅客户端 | Sodium 0.9 / Iris | 25 | `neo-voxy-0.2.18-beta-mc26.1.2-neoforge-client.jar` |

四个发布 JAR 都由各自的 `slimJar` 流程生成：完整依赖包只作为中间文件，不会发布；各版本不需要的
RocksDB/SQLite 原生库、重复模块描述符和已由游戏提供的旧依赖会从最终产物剔除。实际方块、语言、
着色器和运行所需资源不会被删除。

### 功能对比

| 功能 | 1.21.1 联动版 | 1.21.1 客户端版 | 1.20.1 客户端版 | 26.1.2 客户端版 |
|---|:---:|:---:|:---:|:---:|
| 远距离地形 LOD | ✅ | ✅ | ✅ | ✅ |
| LOD 缓存与多级细节 | ✅ | ✅ | ✅ | ✅ |
| 雾气、流体及交界修复 | ✅ | ✅ | ✅ | ✅ |
| Sodium/Embeddium 设置集成 | ✅ | ✅ | ✅ | ✅ |
| Iris/Oculus 光影管线 | ✅ | ✅ | ⚠️ 实验性 | ✅ |
| 圆形 LOD 淡入及专项交接 | ✅ | ✅ | ✅ | ✅ |
| 远距离玩家、乘骑物与动画 | ✅ | — | — | — |
| Create/Sable/节气等专项联动 | ✅ | — | — | — |

`—` 表示该版本不包含专项功能，并不表示基础 LOD 一定与该模组冲突。

### 模组兼容性

| 模组或组件 | 支持版本 | 说明 |
|---|---|---|
| Sodium / Iris | 1.21.1、26.1.2 | 使用表中对应的大版本 |
| Embeddium | 1.20.1 | 基础渲染后端 |
| Oculus | 1.20.1 | 可启动，但光影 LOD 仍有已知渲染问题 |
| Create | 1.21.1 联动版 | 远景列车、轨道、动态结构与动力部件 |
| Sable | 1.21.1 联动版 | 远景物理结构及深度兼容 |
| EclipticSeasons | 1.21.1 联动版 | 远景季节积雪 |
| Domum Ornamentum | 1.21.1 联动版 | 方块实体纹理与独立 LOD 模型 |
| Photon 等自带 LOD 过渡的光影 | 支持光影的版本 | 应关闭 Neo Voxy 的 LOD 淡入，避免双重过渡 |

专项联动只在对应模组已安装时启用，未安装时不会注册其监听器或渲染任务。Create、Sable 和
EclipticSeasons 兼容实现来自 **NHblock**。

### 源码布局

| 目录 | 版本 |
|---|---|
| 仓库根目录 | 1.21.1 NeoForge 联动版 |
| `editions/neoforge-1.21.1-client` | 1.21.1 NeoForge 纯客户端版 |
| `editions/forge-1.20.1-client` | 1.20.1 Forge 纯客户端版 |
| `editions/neoforge-26.1.2-client` | 26.1.2 NeoForge 纯客户端版 |

### 构建

Windows 单独构建：

```powershell
.\scripts\build.ps1 integrations-1.21.1
.\scripts\build.ps1 client-1.21.1
.\scripts\build.ps1 client-1.20.1
.\scripts\build.ps1 client-26.1.2
```

一次构建全部版本：

```powershell
.\scripts\build-all.ps1
```

脚本优先读取 `JAVA_HOME_17`、`JAVA_HOME_21`、`JAVA_HOME_25`，也会识别本地 `D:\Java\<版本>`。
Linux/macOS 可使用 `scripts/build.sh` 与 `scripts/build-all.sh`，并设置相同的 JDK 环境变量。
最终产物统一复制到 `dist/`。GitHub Actions 工作流位于
`.github/workflows/build-multiversion.yml`，会并行构建四个版本，并额外生成包含全部 JAR 的
`neo-voxy-multiversion` artifact。

## English

### Editions and features

| Edition | Installation | Renderer | Shader support | Integrations |
|---|---|---|---|---|
| 1.21.1 NeoForge integrations | Client + server | Sodium 0.8 / Iris | Supported | Create, Sable, seasons, Domum and more |
| 1.21.1 NeoForge client | Client only | Sodium 0.8 / Iris | Supported | None |
| 1.20.1 Forge client | Client only | Embeddium / Oculus | **Experimental; known ghosting issues** | None |
| 26.1.2 NeoForge client | Client only | Sodium 0.9 / Iris | Supported | None |

All editions provide terrain LODs, persistent caches, multi-level detail, renderer settings, fog/fluid
fixes, and the circular vanilla-to-LOD transition. Only the 1.21.1 integrations edition contains
networked distant entities and dedicated mod compatibility. Optional integrations are gated by mod
presence and do not register work when their target mod is absent.

### Building

Build one edition on Windows:

```powershell
.\scripts\build.ps1 integrations-1.21.1
.\scripts\build.ps1 client-1.21.1
.\scripts\build.ps1 client-1.20.1
.\scripts\build.ps1 client-26.1.2
```

Build every edition:

```powershell
.\scripts\build-all.ps1
```

Set `JAVA_HOME_17`, `JAVA_HOME_21`, and `JAVA_HOME_25` when those JDKs are not discoverable. Bash
equivalents are available in `scripts/`. Release JARs are placed in `dist/`; the multiversion GitHub
Actions workflow builds the same four artifacts in parallel and publishes a combined bundle.

## License

See the license file included with each edition. Third-party bundled libraries retain their own licenses.
