package me.cortex.voxy.client.core.rendering;

import me.cortex.voxy.client.config.VoxyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;

/** Cached camera-centred ownership distances shared by all render paths. */
public final class LodBoundaryFade {
    private static final float MIN_VANILLA_RADIUS = 16.0f;
    private static final Distances DISABLED = new Distances(0.0f, 0.0f);
    private static int cachedRenderDistance = Integer.MIN_VALUE;
    private static boolean cachedEnabled;
    private static int cachedLength;
    private static int cachedInset;
    private static int cachedBuffer;
    private static boolean cachedSubmerged;
    private static boolean cachedDetachedCamera;
    private static Distances cachedDistances = DISABLED;

    private LodBoundaryFade() {}

    public record Distances(float fadeStart, float fadeEnd) {
        public boolean enabled() { return this.fadeEnd > this.fadeStart; }
    }

    public static Distances getDistances() {
        VoxyConfig config = VoxyConfig.CONFIG;
        Minecraft minecraft = Minecraft.getInstance();
        int renderDistance = minecraft.options.getEffectiveRenderDistance();
        var camera = minecraft.gameRenderer.getMainCamera();
        boolean submerged = camera.getFluidInCamera() != FogType.NONE;
        boolean detachedCamera = minecraft.player != null
                && camera.getPosition().distanceToSqr(minecraft.player.position()) > 64.0;
        if (renderDistance == cachedRenderDistance
                && config.enableLodBoundaryFade == cachedEnabled
                && config.lodBoundaryFadeLength == cachedLength
                && config.lodBoundaryInset == cachedInset
                && config.lodBoundaryBuffer == cachedBuffer
                && submerged == cachedSubmerged
                && detachedCamera == cachedDetachedCamera) return cachedDistances;

        cachedRenderDistance = renderDistance;
        cachedEnabled = config.enableLodBoundaryFade;
        cachedLength = config.lodBoundaryFadeLength;
        cachedInset = config.lodBoundaryInset;
        cachedBuffer = config.lodBoundaryBuffer;
        cachedSubmerged = submerged;
        cachedDetachedCamera = detachedCamera;
        float vanillaDistance = renderDistance * 16.0f;
        if (!config.enableLodBoundaryFade || submerged || detachedCamera) {
            return cachedDistances = new Distances(vanillaDistance, vanillaDistance);
        }
        float fadeEnd = Math.max(MIN_VANILLA_RADIUS,
                vanillaDistance - config.lodBoundaryInset - config.lodBoundaryBuffer);
        float fadeWidth = Math.min(config.lodBoundaryFadeLength,
                Math.max(0.0f, fadeEnd - MIN_VANILLA_RADIUS));
        if (fadeWidth < 1.0f) return cachedDistances = new Distances(vanillaDistance, vanillaDistance);
        return cachedDistances = new Distances(fadeEnd - fadeWidth, fadeEnd);
    }
}
