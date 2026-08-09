package me.cortex.voxy.client.mixin.iris;

import net.irisshaders.iris.pipeline.SodiumTerrainPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(value = SodiumTerrainPipeline.class, remap = false)
public abstract class MixinSodiumShadowBoundary {
    private static final String MARKER = "voxy_shadow_boundary_distance";
    private static final Pattern MAIN = Pattern.compile("\\bvoid\\s+main\\s*\\(\\s*(?:void\\s*)?\\)");

    @Inject(method = "getShadowVertexShaderSource", at = @At("RETURN"), cancellable = true)
    private void voxy$patchShadowVertex(CallbackInfoReturnable<Optional<String>> cir) {
        SodiumTerrainPipeline pipeline = (SodiumTerrainPipeline) (Object) this;
        if (pipeline.getShadowGeometryShaderSource().isPresent()
                || pipeline.getShadowTessControlShaderSource().isPresent()
                || pipeline.getShadowTessEvalShaderSource().isPresent()) {
            return;
        }
        cir.getReturnValue().map(source -> patch(source, VERTEX_WRAPPER)).ifPresent(source -> cir.setReturnValue(Optional.of(source)));
    }

    @Inject(method = "getShadowFragmentShaderSource", at = @At("RETURN"), cancellable = true)
    private void voxy$patchShadowFragment(CallbackInfoReturnable<Optional<String>> cir) {
        SodiumTerrainPipeline pipeline = (SodiumTerrainPipeline) (Object) this;
        if (pipeline.getShadowGeometryShaderSource().isPresent()
                || pipeline.getShadowTessControlShaderSource().isPresent()
                || pipeline.getShadowTessEvalShaderSource().isPresent()) {
            return;
        }
        cir.getReturnValue().map(source -> patch(source, FRAGMENT_WRAPPER)).ifPresent(source -> cir.setReturnValue(Optional.of(source)));
    }

    private static String patch(String source, String wrapper) {
        if (source.contains(MARKER)) {
            return source;
        }
        Matcher matcher = MAIN.matcher(source);
        return matcher.find()
                ? matcher.replaceFirst("void voxy_shadow_original_main()") + wrapper
                : source;
    }

    private static final String VERTEX_WRAPPER = """

            out float voxy_shadow_boundary_distance;

            void main() {
                voxy_shadow_original_main();
                vec3 voxy_relative_position = _vert_position + u_RegionOffset
                        + _get_draw_translation(_draw_id);
                voxy_shadow_boundary_distance = length(voxy_relative_position.xz);
            }
            """;

    private static final String FRAGMENT_WRAPPER = """

            in float voxy_shadow_boundary_distance;
            uniform float voxyLodBoundaryFadeStart;
            uniform float voxyLodBoundaryFadeEnd;

            float voxy_shadow_dither(ivec2 pixel) {
                ivec2 p = pixel & ivec2(3);
                const int bayer[16] = int[](0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5);
                return (float(bayer[p.y * 4 + p.x]) + 0.5) * (1.0 / 16.0);
            }

            void main() {
                if (voxyLodBoundaryFadeEnd > voxyLodBoundaryFadeStart) {
                    float coverage = smoothstep(voxyLodBoundaryFadeStart,
                            voxyLodBoundaryFadeEnd, voxy_shadow_boundary_distance);
                    if (voxy_shadow_dither(ivec2(gl_FragCoord.xy)) < coverage) {
                        discard;
                    }
                }
                voxy_shadow_original_main();
            }
            """;
}
