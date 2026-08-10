# Neo Voxy · Multiversion

[简体中文](README.md)

Neo Voxy is maintained by **JohnSnow**. It extends [NHblock714/voxy](https://github.com/NHblock714/voxy) with multiversion maintenance, client optimizations, and optional mod integrations.

> [!IMPORTANT]
> If an update causes problems, delete the Neo Voxy configuration and the Voxy cache inside the affected world before retrying.

> [!WARNING]
> Oculus shader support on Minecraft 1.20.1 remains experimental. The native Forge port does not require Connector or Fabric API, but shader-free use is recommended until runtime stability has been verified.

## Supported editions

| Edition | Install side | Renderer | Java | Release file |
|---|---|---|---:|---|
| 1.21.1 NeoForge integrations | Client and server | Sodium 0.8 / Iris | 21 | `neo-voxy-0.3.3-mc1.21.1-neoforge-integrations.jar` |
| 1.21.1 NeoForge client | Client only | Sodium 0.8 / Iris | 21 | `neo-voxy-0.2.18-beta-mc1.21.1-neoforge-client.jar` |
| 1.20.1 Forge client | Client only | Embeddium / Oculus | 17 | `neo-voxy-0.3.3-1.20.1-alpha.1-forge-client.jar` |
| 26.1.2 NeoForge client | Client only | Sodium 0.9 / Iris | 25 | `neo-voxy-0.2.18-beta-mc26.1.2-neoforge-client.jar` |

Release JARs remove unused platform natives, duplicate module descriptors, and build intermediates. Runtime shaders, languages, models, and storage libraries are retained.

## Feature comparison

| Feature | 1.21.1 integrations | 1.21.1 client | 1.20.1 client | 26.1.2 client |
|---|:---:|:---:|:---:|:---:|
| Terrain LODs, detail levels, persistent cache | ✅ | ✅ | ✅ | ✅ |
| Sodium / Embeddium settings integration | ✅ | ✅ | ✅ | ✅ |
| Iris / Oculus shader pipeline | ✅ | ✅ | ⚠️ Experimental | ✅ |
| Environmental/sky fog and fluid fixes | ✅ | ✅ | ✅ | ✅ |
| Circular LOD handoff | ✅ | ✅ | — | ✅ |
| Leaf LOD modes | ✅ | ✅ | — | — |
| Extended chunk requests | ✅ | ✅ | — | — |
| LOD build-pressure control | ✅ | ✅ | — | — |
| World curvature | ✅ | ✅ | — | — |
| Distant beacon beams | ✅ | — | — | — |
| Distant players, vehicles, and animation | ✅ | — | — | — |
| Create, Sable, seasons, and Domum integrations | ✅ | — | — | — |

`—` means that the dedicated feature is not included; it does not necessarily imply incompatibility with basic terrain LOD rendering.

### Main options

- Leaf LOD modes: Minecraft 1.21.1 offers Fast, Balanced, and Quality modes. Balanced rotates textures deterministically, culls hidden internal faces, and retains irregular cutouts; Quality preserves finer transparency.
- Extended chunk requests: asks the game for chunks beyond vanilla distance. High values substantially increase CPU, memory, network, world-generation, and save load. The integrations edition is capped at 48 chunks and disables this option by default.
- LOD build pressure: adjusts per-frame node processing and model-baking budgets from maximum FPS to maximum catch-up speed.
- Circular LOD handoff: blends the vanilla/LOD boundary. Disable it when a shader pack already implements its own LOD transition, such as Photon, to avoid double transitions, noise, or shadow seams.
- Fog and clouds: environmental fog, sky-fog distance, fog intensity/density, and adaptive cloud distance are available depending on edition.
- Subdivision size: controls the screen-space threshold for finer LODs. Lower values improve detail at higher build and rendering cost.

## Mod compatibility

| Mod or component | Editions | Notes |
|---|---|---|
| Sodium / Iris | 1.21.1 and 26.1.2 | Use the renderer major version appropriate for the Minecraft version |
| Embeddium | 1.20.1 | Required native Forge renderer |
| Oculus | 1.20.1 | Optional; shader support remains experimental |
| Create | 1.21.1 integrations | Distant trains, tracks, contraptions, and kinetic components |
| Sable | 1.21.1 integrations | Distant physics objects and depth integration |
| Ecliptic Seasons | 1.21.1 integrations | Seasonal snow in distant terrain |
| Domum Ornamentum | 1.21.1 integrations | Special block coloring and dedicated LOD models |
| Photon and other shaders with native LOD handoff | Shader-capable editions | Disable Neo Voxy's circular handoff to prevent duplicate transitions |

Optional integrations activate only when the corresponding mod is installed. Create, Sable, and seasonal compatibility originate from **NHblock**.

## Building

Build one edition on Windows:

```powershell
.\scripts\build.ps1 integrations-1.21.1
.\scripts\build.ps1 client-1.21.1
.\scripts\build.ps1 client-1.20.1
.\scripts\build.ps1 client-26.1.2
```

Build all editions:

```powershell
.\scripts\build-all.ps1
```

The scripts prefer `JAVA_HOME_17`, `JAVA_HOME_21`, and `JAVA_HOME_25`. Linux/macOS equivalents are available as `scripts/build.sh` and `scripts/build-all.sh`. Final artifacts are copied to `dist/`; GitHub Actions builds the four editions in parallel and publishes one `neo-voxy-multiversion` bundle.

## License

See the license file shipped with each edition. Bundled third-party libraries retain their respective licenses.
