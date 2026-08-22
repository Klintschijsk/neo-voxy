# Neo Voxy · Multiversion

[简体中文](README.md)

Neo Voxy is maintained by **JohnSnow**. It extends [NHblock714/voxy](https://github.com/NHblock714/voxy) with multiversion maintenance, client optimizations, and optional mod integrations.

> [!IMPORTANT]
> If an update causes problems, delete the Neo Voxy configuration and the Voxy cache inside the affected world before retrying.


## Supported editions

| Edition | Install side | Renderer | Java | Release file |
|---|---|---|---:|---|
| 1.21.1 NeoForge integrations | Client and server | Sodium 0.8 / Iris | 21 | `neo-voxy-0.4.2-beta.1-mc1.21.1-neoforge-integrations.jar` |
| 1.21.1 NeoForge client | Client only | Sodium 0.8 / Iris | 21 | `neo-voxy-0.3.0-mc1.21.1-neoforge-client.jar` |
| 1.20.1 Forge client | Client only | Embeddium / Oculus | 17 | `neo-voxy-0.3.1-forge-client.jar` |
| 26.1.2 NeoForge client | Client only | Sodium 0.9 / Iris | 25 | `neo-voxy-0.3.1-mc26.1.2-neoforge-client.jar` |

Release JARs remove unused platform natives, duplicate module descriptors, and build intermediates. Runtime shaders, languages, models, and storage libraries are retained.

## Feature and compatibility comparison

| Feature, mod, or component | 1.21.1 integrations | 1.21.1 client | 1.20.1 client | 26.1.2 client | Notes |
|---|:---:|:---:|:---:|:---:|---|
| Terrain LODs, detail levels, persistent cache | ✅ | ✅ | ✅ | ✅ | Core distant-terrain support |
| Sodium / Embeddium settings integration | ✅ | ✅ | ✅ | ✅ | Neo Voxy settings entry |
| Iris / Oculus shader pipeline | ✅ | ✅ | ✅ | ✅ | Shaders are supported on 1.20.1; compatibility may vary by shader pack |
| Environmental/sky fog and fluid fixes | ✅ | ✅ | ✅ | ✅ | Includes required medium masks such as underwater fog |
| Circular LOD handoff | ✅ | ✅ | ❌ | ✅ | Not planned for 1.20.1; disable in other editions when the shader pack supplies its own LOD transition |
| Crossed ground-plant models | ✅ | ✅ | ✅ | ✅ | Lightweight centred crossed models |
| Leaf LOD modes | ✅ | ✅ | ✅ | ✅ | Fast, Balanced, and Quality modes |
| Extended chunk requests (single-player, max 48) | ✅ | ✅ | ✅ | ✅ | Disabled by default |
| LOD biome water-colour blending | ✅ | ✅ | ✅ | ✅ | Built with the model; no per-tick traversal |
| LOD build-pressure control | ✅ | ✅ | ✅ | ✅ | Prioritise frame rate or catch-up speed |
| World curvature | ✅ | ✅ | ✅ | ✅ | Implemented in the GPU vertex stage |
| Distant beacon beams | ✅ | — | — | — | Width scales with distance |
| Distant players, vehicles, and animation | ✅ | — | — | — | Integrations-edition feature |
| Sodium / Iris | ✅ | ✅ | — | ✅ | Use the renderer major version appropriate for Minecraft |
| Embeddium / Oculus | — | — | ✅ | — | Embeddium is the renderer; Oculus provides shader support |
| Create | ✅ | — | — | — | Distant trains, tracks, contraptions, and kinetic components |
| Sable | ✅ | — | — | — | Distant physics objects and depth integration |
| Ecliptic Seasons | ✅ | — | — | — | Seasonal snow in distant terrain |
| Domum Ornamentum | 🧪 | — | — | — | Preliminary: special colouring and dedicated LOD models |
| LittleTiles | 🧪 | — | — | — | Preliminary: persistent lightweight 1/8-block LOD meshes for static structures |

`✅` means supported, `🧪` means preliminary compatibility, and `—` means no dedicated feature or not applicable; it does not necessarily imply incompatibility with basic terrain LOD rendering. Optional integrations activate only when the corresponding mod is installed. Create, Sable, and seasonal compatibility originate from **NHblock**.

### Main options

- Ground plants and leaves: all four editions provide centered crossed-plant LODs plus Fast, Balanced, and Quality leaf modes. Balanced culls hidden internal faces while keeping stable asymmetric cutouts. Leaves bypass alpha fading and hand directly between vanilla and LOD models, preventing the disappear/reappear cycle.
- Extended chunk requests: based on the approach used by [FakeSight](https://github.com/MoePus/fakesight), asks for chunks beyond vanilla distance in single-player. It is disabled by default and capped at 48 chunks in all four editions. Expansion pauses while moving and resumes gradually when stationary. High values can still increase CPU, memory, world-generation, and save load substantially.
- Biome water-colour blending: all four editions smooth LOD water colours across biome borders while models are built. Results use a compact palette and require no per-tick world traversal.
- LOD build pressure: adjusts per-frame node processing and model-baking budgets from maximum FPS to maximum catch-up speed.
- Circular LOD handoff: editions containing this feature use 3D camera distance and world-stable dithering. Water and leaves use dedicated non-alpha handoff paths. Disable it when a shader pack already implements its own LOD transition, such as Photon, to avoid duplicate transitions, noise, or shadow seams.
- Fog and effects: shader-free fog on 1.20.1 and 1.21.1 scales against the LOD radius while preserving required medium masks such as underwater fog. Distant LODs no longer show through Blindness or Darkness. Version 26.1.2 uses the newer native fog path.
- Model and fluid quality: per-face mip generation and exact per-pixel tint masks reduce grass-side and waterlogged-plant colour errors; nearest rounding, independent fluid boundaries, and biome-colour handling improve distant water and terrain.
- Experimental Lite LOD shading: the 1.21.1 integrations build uses paired Lite programs and atomically falls back if loading or compilation fails, the version is unsupported, or the transition is unsafe. Eclipse Shader 482 is supported by a built-in NeoVoxy patch and does not require shader-pack changes; Complementary Unbound r5.8.1 + Euphoria Patches 1.9.3 uses a separate overlay.
- Subdivision size: controls the screen-space threshold for finer LODs. Lower values improve detail at higher build and rendering cost.
- World curvature: all four editions curve only the LOD beyond vanilla distance in the GPU vertex stage; 0 disables it, with no chunk scan or per-tick traversal.
- Join message: shown whenever a server or single-player world is entered, enabled by default and removable from the Neo Voxy Sodium/Embeddium settings.

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
