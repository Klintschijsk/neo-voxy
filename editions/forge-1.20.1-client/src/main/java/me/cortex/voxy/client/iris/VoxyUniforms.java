package me.cortex.voxy.client.iris;

import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.rendering.Viewport;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

import java.util.function.Supplier;

import static net.irisshaders.iris.gl.uniform.UniformUpdateFrequency.PER_FRAME;

public class VoxyUniforms {
    private static final Matrix4f viewProjection = new Matrix4f();
    private static final Matrix4f previousViewProjection = new Matrix4f();
    private static final Matrix4f modelView = new Matrix4f();
    private static final Matrix4f previousModelView = new Matrix4f();
    private static final Matrix4f projection = new Matrix4f();
    private static final Matrix4f previousProjection = new Matrix4f();
    private static Viewport<?> capturedViewport;
    private static int capturedFrame = Integer.MIN_VALUE;

    public static void capture(Viewport<?> viewport) {
        boolean newSequence = capturedViewport != viewport || capturedFrame == Integer.MIN_VALUE;
        boolean newFrame = !newSequence && capturedFrame != viewport.frameId;

        if (newFrame) {
            previousViewProjection.set(viewProjection);
            previousModelView.set(modelView);
            previousProjection.set(projection);
        }

        viewProjection.set(viewport.MVP);
        modelView.set(viewport.modelView);
        projection.set(viewport.projection);

        if (newSequence) {
            previousViewProjection.set(viewProjection);
            previousModelView.set(modelView);
            previousProjection.set(projection);
        }

        capturedViewport = viewport;
        capturedFrame = viewport.frameId;
    }

    public static void reset() {
        capturedViewport = null;
        capturedFrame = Integer.MIN_VALUE;
    }

    public static Matrix4f getViewProjection() { return new Matrix4f(viewProjection); }
    public static Matrix4f getPreviousViewProjection() { return new Matrix4f(previousViewProjection); }
    public static Matrix4f getModelView() { return new Matrix4f(modelView); }
    public static Matrix4f getPreviousModelView() { return new Matrix4f(previousModelView); }
    public static Matrix4f getProjection() { return new Matrix4f(projection); }
    public static Matrix4f getPreviousProjection() { return new Matrix4f(previousProjection); }

    public static void addUniforms(UniformHolder uniforms) {
        uniforms
                .uniform1i(PER_FRAME, "vxRenderDistance", ()-> VoxyConfig.CONFIG.sectionRenderDistance*32)//In chunks
                .uniform1f(PER_FRAME, "voxyLodBoundaryFadeStart", VoxyUniforms::getShadowFadeStart)
                .uniform1f(PER_FRAME, "voxyLodBoundaryFadeEnd", VoxyUniforms::getShadowFadeEnd)
                .uniformMatrix(PER_FRAME, "vxViewProj", VoxyUniforms::getViewProjection)
                .uniformMatrix(PER_FRAME, "vxViewProjInv", new Inverted(VoxyUniforms::getViewProjection))
                .uniformMatrix(PER_FRAME, "vxViewProjPrev", VoxyUniforms::getPreviousViewProjection)
                .uniformMatrix(PER_FRAME, "vxModelView", VoxyUniforms::getModelView)
                .uniformMatrix(PER_FRAME, "vxModelViewInv", new Inverted(VoxyUniforms::getModelView))
                .uniformMatrix(PER_FRAME, "vxModelViewPrev", VoxyUniforms::getPreviousModelView)
                .uniformMatrix(PER_FRAME, "vxProj", VoxyUniforms::getProjection)
                .uniformMatrix(PER_FRAME, "vxProjInv", new Inverted(VoxyUniforms::getProjection))
                .uniformMatrix(PER_FRAME, "vxProjPrev", VoxyUniforms::getPreviousProjection);

        if (IrisShaderPatch.IMPERSONATE_DISTANT_HORIZONS) {
            uniforms
                    .uniform1f(PER_FRAME, "dhNearPlane", ()->16)//Presently hardcoded in voxy
                    .uniform1f(PER_FRAME, "dhFarPlane", ()->16*3000)//Presently hardcoded in voxy

                    .uniform1i(PER_FRAME, "dhRenderDistance", ()-> VoxyConfig.CONFIG.sectionRenderDistance*32*16)//In blocks
                    .uniformMatrix(PER_FRAME, "dhProjection", VoxyUniforms::getProjection)
                    .uniformMatrix(PER_FRAME, "dhProjectionInverse", new Inverted(VoxyUniforms::getProjection))
                    .uniformMatrix(PER_FRAME, "dhPreviousProjection", VoxyUniforms::getPreviousProjection);
        }
    }

    private static float getShadowFadeEnd() {
        int radius = Math.max(16, (Minecraft.getInstance().options.getEffectiveRenderDistance() - 2) * 16);
        return radius;
    }

    private static float getShadowFadeStart() {
        if (!VoxyConfig.CONFIG.enableShaderShadowFade) {
            return getShadowFadeEnd();
        }
        return Math.max(0.0f, getShadowFadeEnd() - Math.max(4, VoxyConfig.CONFIG.shaderShadowFadeLength));
    }




    private record Inverted(Supplier<Matrix4f> parent) implements Supplier<Matrix4f> {
        private Inverted(Supplier<Matrix4f> parent) {
            this.parent = parent;
        }

        public Matrix4f get() {
            Matrix4f copy = new Matrix4f(this.parent.get());
            copy.invert();
            return copy;
        }

        public Supplier<Matrix4f> parent() {
            return this.parent;
        }
    }

}
