package me.cortex.voxy.client.core.compat.eclipticseasons;

import com.teamtea.eclipticseasons.api.EclipticSeasonsApi;
import com.teamtea.eclipticseasons.client.util.ClientCon;
import com.teamtea.eclipticseasons.common.core.map.MapChecker;
import com.teamtea.eclipticseasons.config.CommonConfig;
import java.lang.ref.WeakReference;
import me.cortex.voxy.client.config.VoxyConfig;
import java.lang.reflect.Method;
import java.util.function.IntConsumer;
import me.cortex.voxy.common.voxelization.ILightingSupplier;
import me.cortex.voxy.common.voxelization.VoxelizedSection;
import me.cortex.voxy.common.world.WorldEngine;
import me.cortex.voxy.common.world.WorldSection;
import me.cortex.voxy.common.world.other.Mapper;
import me.cortex.voxy.commonImpl.VoxyInstance;
import me.cortex.voxy.commonImpl.WorldIdentifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

public class VoxyTool {
    private static final int maxBlockId = 1048575;
    private static final int INITIAL_REFRESH_DELAY_TICKS = 200;
    private static final int ENGINE_RETRY_TICKS = 40;
    // Do not keep a disconnected ClientLevel alive merely because no further level tick can clear it.
    private static WeakReference<Level> initialRefreshLevel = new WeakReference<>(null);
    private static int initialRefreshCountdown = INITIAL_REFRESH_DELAY_TICKS;
    private static boolean initialRefreshStarted;

    public static boolean isVoxyTest() {
        return VoxyConfig.CONFIG.eclipticSeasonsSnowLod;
    }

    public static int changeBlockId(int blockId, Mapper stateMapper, int i, VoxelizedSection section, ILightingSupplier lightSupplier, int biomeId) {
        if (!VoxyTool.isVoxyTest()) {
            return blockId;
        }
        int maxBlockId = 1048575;
        BlockState state = stateMapper.getBlockStateFromBlockId(blockId);
        if (MapChecker.getDefaultBlockTypeFlag((BlockState)state) > 0) {
            BlockPos offset = SectionPos.of((int)section.x, (int)section.y, (int)section.z).origin().offset(i & 0xF, i >> 8 & 0xF, i >> 4 & 0xF);
            Level level = ClientCon.getUseLevel();
            if (level != null) {
                IVoxyAboveLightingSupplier supplier;
                byte supply;
                int skyLight;
                if (MapChecker.isLoaded((Level)level, (int)section.x, (int)section.z)) {
                    if (EclipticSeasonsApi.getInstance().isSnowyBlock(level, state, offset)) {
                        blockId = maxBlockId - blockId;
                    }
                } else if (lightSupplier instanceof IVoxyAboveLightingSupplier && (skyLight = (supply = (supplier = (IVoxyAboveLightingSupplier)lightSupplier).supply(i & 0xF, (i >> 8 & 0xF) + 1, i >> 4 & 0xF)) & 0xFF & 0xF) > 9 && (!((Boolean)CommonConfig.Snow.notSnowyNearGlowingBlock.get()).booleanValue() || ((supply & 0xFF) >> 4 & 0xF) < CommonConfig.Snow.notSnowyNearGlowingBlockLevel.getAsInt())) {
                    BlockState aboveState = supplier.getBlockState(i & 0xF, (i >> 8 & 0xF) + 1, i >> 4 & 0xF);
                    boolean isLight = true;
                    int flag = MapChecker.getDefaultBlockTypeFlag((BlockState)state);
                    if (MapChecker.leaveLike((int)flag)) {
                        boolean specialLeaves;
                        boolean bl = specialLeaves = aboveState.is(state.getBlock()) && (Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.isOpaque().test(aboveState) || MapChecker.extraSnowPassable((BlockState)aboveState));
                        if (specialLeaves) {
                            isLight = (Boolean)CommonConfig.Snow.snowyTree.get();
                        }
                    } else if (MapChecker.extraSnowPassable((BlockState)state)) {
                        boolean bl = isLight = !MapChecker.extraSnowPassable((BlockState)aboveState);
                    }
                    if (isLight) {
                        String biome = stateMapper.getBiomeEntries()[biomeId].biome;
                        ResourceKey holderKey = ResourceKey.create((ResourceKey)Registries.BIOME, (ResourceLocation)ResourceLocation.parse((String)biome));
                        Holder.Reference holder = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(holderKey);
                        if (MapChecker.shouldSnowAtBiome((Level)level, (Biome)((Biome)holder.value()), (BlockState)state, (RandomSource)level.getRandom(), (long)state.getSeed(offset), (BlockPos)offset)) {
                            blockId = maxBlockId - blockId;
                        }
                    }
                }
            }
        }
        return blockId;
    }

    public static int fixId(Mapper mapper, int blockId) {
        return VoxyTool.fixId(mapper, blockId, VoxyTool::emptyConsumer);
    }

    private static void emptyConsumer(int i) {
    }

    public static int fixId(Mapper mapper, int blockId, IntConsumer consumer) {
        int blockStateCount = mapper.getBlockStateCount();
        if (blockId < blockStateCount) {
            return blockId;
        }
        if ((blockId = 1048575 - blockId) < blockStateCount) {
            consumer.accept(blockId);
            return blockId;
        }
        return 1048575 - blockId;
    }

    public static WorldEngine getWorld(Level level) {
        return VoxyTool.getVoxyInstance().getNullable(WorldIdentifier.of((Level)level));
    }

    public static Mapper getMapper(Level level) {
        WorldEngine world = VoxyTool.getWorld(level);
        return world == null ? null : world.getMapper();
    }

    public static int getSkyLightFromBlockId(long blockId) {
        return Mapper.getLightId((long)blockId) % 16;
    }

    public static WorldSection getWorldSection(WorldEngine into, SectionPos section) {
        int lvl = 0;
        return into.acquireIfExists(lvl, section.x() >> lvl + 1, section.y() >> lvl + 1, section.z() >> lvl + 1);
    }

    public static WorldSection getWorldSection(Level level, SectionPos section) {
        WorldEngine world = VoxyTool.getWorld(level);
        return world == null ? null : VoxyTool.getWorldSection(world, section);
    }

    public static void tryUpdate() {
        if (!VoxyTool.isVoxyTest()) {
            return;
        }
        if (!VoxyConfig.CONFIG.eclipticSeasonsLodAutoReload) {
            return;
        }
        Level level = ClientCon.getUseLevel();
        if (level == null) {
            initialRefreshLevel = new WeakReference<>(null);
            initialRefreshStarted = false;
            initialRefreshCountdown = INITIAL_REFRESH_DELAY_TICKS;
            return;
        }

        if (level != initialRefreshLevel.get()) {
            initialRefreshLevel = new WeakReference<>(level);
            initialRefreshStarted = false;
            // Let the remote Voxy store open and ingest its first batches before walking it. Starting
            // at ClientLevel construction can finish against an empty store and never reach server LOD.
            initialRefreshCountdown = INITIAL_REFRESH_DELAY_TICKS;
        }

        boolean seasonChanged = ClientCon.getAgent().isSnowChange();
        if (!seasonChanged && initialRefreshStarted) {
            return;
        }
        if (!seasonChanged && initialRefreshCountdown-- > 0) {
            return;
        }
        if (SeasonalSnowRefresher.isRunning()) {
            return;
        }

        WorldEngine engine = WorldIdentifier.ofEngineNullable(level);
        if (engine == null || !engine.isLive()) {
            initialRefreshCountdown = ENGINE_RETRY_TICKS;
            return;
        }

        if (seasonChanged) {
            ClientCon.agent.setSnowChange(false);
        }
        initialRefreshStarted = true;
        SeasonalSnowRefresher.start(level, engine);
    }

    @Nullable
    private static VoxyInstance getVoxyInstance() {
        VoxyInstance instance = null;
        try {
            Class<?> clazz = Class.forName("me.cortex.voxy.commonImpl.VoxyCommon");
            Method method = clazz.getDeclaredMethod("getInstance", new Class[0]);
            instance = (VoxyInstance)method.invoke(null, new Object[0]);
        }
        catch (Exception exception) {
            // empty catch block
        }
        return instance;
    }
}

