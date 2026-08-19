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
   @Unique private static final int VOXY_EXPANSION_INTERVAL = 40;
   @Unique private static final int VOXY_MOVEMENT_PAUSE = 80;
   @Unique private int voxy$current = -1;
   @Unique private int voxy$expansionTicks;
   @Unique private int voxy$pauseTicks;
   @Unique private long voxy$lastChunk = Long.MIN_VALUE;
   @Unique private int voxy$lastDimension;

   @ModifyArg(method = "tickServer", at = @At(value = "INVOKE",
      target = "Ljava/lang/Math;max(II)I", ordinal = 0), index = 1, require = 0)
   private int voxy$extendedDistance(int original) {
      if (!VoxyConfig.CONFIG.enableExtendedRequestDistance || !VoxyConfig.CONFIG.isRenderingEnabled()) {
         voxy$reset();
         return original;
      }
      int target = Math.max(original, VoxyConfig.CONFIG.getRequestDistance());
      voxy$current = Math.max(original, Math.min(voxy$current < 0 ? original : voxy$current, target));
      var players = ((IntegratedServer)(Object)this).getPlayerList().getPlayers();
      if (players.isEmpty()) return voxy$current;
      var player = players.getFirst();
      ChunkPos chunk = player.chunkPosition();
      long chunkKey = chunk.pack();
      int dimension = player.level().dimension().hashCode();
      if (chunkKey != voxy$lastChunk || dimension != voxy$lastDimension) {
         voxy$lastChunk = chunkKey;
         voxy$lastDimension = dimension;
         voxy$pauseTicks = VOXY_MOVEMENT_PAUSE;
         voxy$expansionTicks = 0;
      }
      if (voxy$pauseTicks > 0) --voxy$pauseTicks;
      else if (voxy$current < target && ++voxy$expansionTicks >= VOXY_EXPANSION_INTERVAL) {
         ++voxy$current;
         voxy$expansionTicks = 0;
      }
      return voxy$current;
   }

   @Unique private void voxy$reset() {
      voxy$current = -1;
      voxy$expansionTicks = voxy$pauseTicks = 0;
      voxy$lastChunk = Long.MIN_VALUE;
      voxy$lastDimension = 0;
   }
}
