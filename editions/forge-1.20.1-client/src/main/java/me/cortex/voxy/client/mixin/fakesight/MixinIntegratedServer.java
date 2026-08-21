package me.cortex.voxy.client.mixin.fakesight;

import me.cortex.voxy.client.config.VoxyConfig;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer {
    @Unique private static final int VOXY_EXPANSION_INTERVAL_TICKS = 40;
    @Unique private static final int VOXY_EXPANSION_STEP = 8;
    @Unique private static final int VOXY_MOVEMENT_PAUSE_TICKS = 80;
    @Unique private int voxy$currentRequestDistance = -1;
    @Unique private int voxy$expansionTicks;
    @Unique private int voxy$movementPauseTicks;
    @Unique private long voxy$lastPlayerChunk = Long.MIN_VALUE;
    @Unique private int voxy$lastDimensionHash;

    @ModifyArg(method = "tickServer", at = @At(value = "INVOKE",
            target = "Ljava/lang/Math;max(II)I", ordinal = 0),
            index = 1, require = 1)
    private int voxy$extendIntegratedDistance(int originalDistance) {
        if (!VoxyConfig.CONFIG.enableExtendedRequestDistance) {
            voxy$reset();
            return originalDistance;
        }
        int target = Math.max(originalDistance, VoxyConfig.CONFIG.getRequestDistance());
        voxy$currentRequestDistance = Math.max(originalDistance,
                Math.min(voxy$currentRequestDistance < 0 ? originalDistance : voxy$currentRequestDistance, target));
        var players = ((IntegratedServer)(Object)this).getPlayerList().getPlayers();
        if (players.isEmpty()) return voxy$currentRequestDistance;
        var player = players.get(0);
        ChunkPos chunk = player.chunkPosition();
        long chunkKey = ChunkPos.asLong(chunk.x, chunk.z);
        int dimensionHash = player.serverLevel().dimension().hashCode();
        if (chunkKey != voxy$lastPlayerChunk || dimensionHash != voxy$lastDimensionHash) {
            voxy$lastPlayerChunk = chunkKey;
            voxy$lastDimensionHash = dimensionHash;
            voxy$movementPauseTicks = VOXY_MOVEMENT_PAUSE_TICKS;
            voxy$expansionTicks = 0;
        }
        if (voxy$movementPauseTicks > 0) {
            --voxy$movementPauseTicks;
        } else if (voxy$currentRequestDistance < target && ++voxy$expansionTicks >= VOXY_EXPANSION_INTERVAL_TICKS) {
            voxy$currentRequestDistance = Math.min(target, voxy$currentRequestDistance + VOXY_EXPANSION_STEP);
            voxy$expansionTicks = 0;
        }
        return voxy$currentRequestDistance;
    }

    @Unique private void voxy$reset() {
        voxy$currentRequestDistance = -1;
        voxy$expansionTicks = voxy$movementPauseTicks = 0;
        voxy$lastPlayerChunk = Long.MIN_VALUE;
        voxy$lastDimensionHash = 0;
    }
}
