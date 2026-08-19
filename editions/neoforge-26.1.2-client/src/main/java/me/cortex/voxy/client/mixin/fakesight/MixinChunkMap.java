package me.cortex.voxy.client.mixin.fakesight;

import me.cortex.voxy.client.config.VoxyConfig;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ChunkMap.class)
public abstract class MixinChunkMap {
   @ModifyArg(method = "setServerViewDistance", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/util/Mth;clamp(III)I"), index = 2, require = 0)
   private int voxy$extendLimit(int vanillaLimit) {
      return VoxyConfig.CONFIG.enableExtendedRequestDistance && VoxyConfig.CONFIG.isRenderingEnabled()
         ? Math.max(vanillaLimit, 49) : vanillaLimit;
   }
}
