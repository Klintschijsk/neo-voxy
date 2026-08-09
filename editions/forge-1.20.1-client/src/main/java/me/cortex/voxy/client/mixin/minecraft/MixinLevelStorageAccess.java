package me.cortex.voxy.client.mixin.minecraft;

import me.cortex.voxy.client.core.IGetVoxyRenderSystem;
import me.cortex.voxy.commonImpl.VoxyCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
public class MixinLevelStorageAccess {
    @Inject(method = "deleteLevel", at = @At("HEAD"))
    private void voxy$closeStorageBeforeDeletingWorld(CallbackInfo ci) {
        var levelRenderer = Minecraft.getInstance().levelRenderer;
        if (levelRenderer instanceof IGetVoxyRenderSystem renderHook) {
            renderHook.shutdownRenderer();
        }
        VoxyCommon.shutdownInstance();
    }
}
