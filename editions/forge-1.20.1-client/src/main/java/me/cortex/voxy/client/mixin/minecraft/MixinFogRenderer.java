package me.cortex.voxy.client.mixin.minecraft;

import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.IGetVoxyRenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {
    @Inject(
        method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZF)V",
        at = @At("TAIL"),
        cancellable = true
    )
    private static void voxy$overrideFog(
        Camera camera,
        FogMode fogMode,
        float viewDistance,
        boolean thickFog,
        float tickDelta,
        CallbackInfo ci
    ) {
        var access = (IGetVoxyRenderSystem) Minecraft.getInstance().levelRenderer;
        if (access == null || access.getVoxyRenderSystem() == null) {
            return;
        }

        // Water, lava, powder snow, blindness and darkness are vision masks. Keep vanilla's live
        // ramp so the LOD can use the same near/far values instead of becoming crystal clear.
        boolean restricted = camera.getFluidInCamera() != FogType.NONE
                || camera.getEntity() instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS));
        if (fogMode == FogMode.FOG_TERRAIN && restricted) {
            return;
        }

        if (fogMode == FogMode.FOG_SKY && RenderSystem.getShaderFogEnd() >= 10.0f) {
            RenderSystem.setShaderFogStart(0.0f);
            RenderSystem.setShaderFogEnd(Math.max(0, VoxyConfig.CONFIG.skyFogDistance));
            return;
        }

        if (fogMode == FogMode.FOG_TERRAIN && RenderSystem.getShaderFogEnd() >= 10.0f) {
            RenderSystem.setShaderFogStart(999999999);
            RenderSystem.setShaderFogEnd(999999999);
        }
    }
}
