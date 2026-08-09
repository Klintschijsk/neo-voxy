package me.cortex.voxy.client.core;

import com.mojang.blaze3d.systems.RenderSystem;
import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.gl.GlFramebuffer;
import me.cortex.voxy.client.core.gl.GlTexture;
import me.cortex.voxy.client.core.gl.shader.Shader;
import me.cortex.voxy.client.core.gl.shader.ShaderType;
import me.cortex.voxy.client.core.rendering.Viewport;
import me.cortex.voxy.client.core.rendering.hierachical.AsyncNodeManager;
import me.cortex.voxy.client.core.rendering.hierachical.HierarchicalOcclusionTraverser;
import me.cortex.voxy.client.core.rendering.hierachical.NodeCleaner;
import me.cortex.voxy.client.core.rendering.post.FullscreenBlit;
import me.cortex.voxy.client.core.rendering.util.DepthFramebuffer;
import me.cortex.voxy.client.core.util.IrisUtil;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.util.function.BooleanSupplier;

import static org.lwjgl.opengl.ARBComputeShader.glDispatchCompute;
import static org.lwjgl.opengl.ARBShaderImageLoadStore.glBindImageTexture;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11C.GL_NEAREST;
import static org.lwjgl.opengl.GL11C.GL_RGBA8;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL15.GL_READ_WRITE;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL43.GL_DEPTH_STENCIL_TEXTURE_MODE;
import static org.lwjgl.opengl.GL45C.glBindTextureUnit;
import static org.lwjgl.opengl.GL45C.glClearNamedFramebufferfv;
import static org.lwjgl.opengl.GL45C.glTextureParameterf;

public class NormalRenderPipeline extends AbstractRenderPipeline {
    private GlTexture colourTex;
    private GlTexture colourSSAOTex;
    private final GlFramebuffer fbSSAO = new GlFramebuffer();
    private final DepthFramebuffer fb = new DepthFramebuffer(GL_DEPTH24_STENCIL8);

    private final FullscreenBlit finalBlit;

    private final Shader ssaoCompute = Shader.make()
            .add(ShaderType.COMPUTE, "voxy:post/ssao.comp")
            .compile();

    protected NormalRenderPipeline(AsyncNodeManager nodeManager, NodeCleaner nodeCleaner, HierarchicalOcclusionTraverser traversal, BooleanSupplier frexSupplier) {
        super(nodeManager, nodeCleaner, traversal, frexSupplier);
        this.finalBlit = new FullscreenBlit("voxy:post/blit_texture_depth_cutout.frag",
                a->a.define("USE_ENV_FOG").define("EMIT_COLOUR"));
    }

    @Override
    protected int setup(Viewport<?> viewport, int sourceFB, int srcWidth, int srcHeight) {
        if (this.colourTex == null || this.colourTex.getHeight() != viewport.height || this.colourTex.getWidth() != viewport.width) {
            if (this.colourTex != null) {
                this.colourTex.free();
                this.colourSSAOTex.free();
            }
            this.fb.resize(viewport.width, viewport.height);

            this.colourTex = new GlTexture().store(GL_RGBA8, 1, viewport.width, viewport.height);
            this.colourSSAOTex = new GlTexture().store(GL_RGBA8, 1, viewport.width, viewport.height);

            this.fb.framebuffer.bind(GL_COLOR_ATTACHMENT0, this.colourTex).verify();
            this.fbSSAO.bind(GL_DEPTH_STENCIL_ATTACHMENT, this.fb.getDepthTex()).bind(GL_COLOR_ATTACHMENT0, this.colourSSAOTex).verify();


            glTextureParameterf(this.colourTex.id, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameterf(this.colourTex.id, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTextureParameterf(this.colourSSAOTex.id, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameterf(this.colourSSAOTex.id, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTextureParameterf(this.fb.getDepthTex().id, GL_DEPTH_STENCIL_TEXTURE_MODE, GL_DEPTH_COMPONENT);
        }

        // Oculus' fallback target cannot use destination depth to reject pixels left over in these
        // persistent colour textures. Clear them only for that fallback path.
        if (IrisUtil.irisShaderPackEnabled()) {
            try (var stack = MemoryStack.stackPush()) {
                var clear = stack.mallocFloat(4);
                clear.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f);
                glClearNamedFramebufferfv(this.fb.framebuffer.id, GL_COLOR, 0, clear);
                glClearNamedFramebufferfv(this.fbSSAO.id, GL_COLOR, 0, clear);
            }
        }

        this.initDepthStencil(sourceFB, this.fb.framebuffer.id, viewport.width, viewport.height, viewport.width, viewport.height);

        return this.fb.getDepthTex().id;
    }

    @Override
    protected void postOpaquePreTranslucent(Viewport<?> viewport) {
        this.ssaoCompute.bind();
        try (var stack = MemoryStack.stackPush()) {
            long ptr = stack.nmalloc(4*4*4);
            viewport.MVP.getToAddress(ptr);
            nglUniformMatrix4fv(3, 1, false, ptr);//MVP
            viewport.MVP.invert(new Matrix4f()).getToAddress(ptr);
            nglUniformMatrix4fv(4, 1, false, ptr);//invMVP
        }


        glBindImageTexture(0, this.colourSSAOTex.id, 0, false,0, GL_READ_WRITE, GL_RGBA8);
        glBindTextureUnit(1, this.fb.getDepthTex().id);
        glBindTextureUnit(2, this.colourTex.id);

        glDispatchCompute((viewport.width+31)/32, (viewport.height+31)/32, 1);

        glBindFramebuffer(GL_FRAMEBUFFER, this.fbSSAO.id);
    }

    @Override
    protected void finish(Viewport<?> viewport, int sourceFrameBuffer, int srcWidth, int srcHeight) {
        this.finalBlit.bind();

        glBindTextureUnit(3, this.colourSSAOTex.id);

        float fogNear = 0.0f;
        float fogFar = 0.0f;
        float fogIntensity = 0.0f;
        float fogDensity = 0.0f;
        int linearFog = 0;

        var mc = Minecraft.getInstance();
        boolean inMedium = mc.gameRenderer != null
                && mc.gameRenderer.getMainCamera().getFluidInCamera()
                    != net.minecraft.world.level.material.FogType.NONE;
        if (inMedium) {
            fogNear = VoxyRenderSystem.getTerrainFogStartAtRender();
            fogFar = VoxyRenderSystem.getTerrainFogEndAtRender();
            if (fogFar > fogNear) {
                fogIntensity = 1.0f;
                linearFog = 1;
            }
        } else if (VoxyConfig.CONFIG.useEnvironmentalFog) {
            float lodRadius = VoxyConfig.CONFIG.sectionRenderDistance * 32.0f * 16.0f;
            fogFar = lodRadius * Math.max(5, Math.min(200, VoxyConfig.CONFIG.fogDistancePercent)) / 100.0f;
            fogNear = fogFar * 0.5f;
            fogIntensity = Math.max(0.0f, Math.min(1.0f, VoxyConfig.CONFIG.fogIntensity));
            fogDensity = Math.max(0.0f, Math.min(1.0f, VoxyConfig.CONFIG.fogDensity));
        }

        float[] fogColor = RenderSystem.getShaderFogColor();
        glUniform2f(4, fogNear, fogFar);
        glUniform4f(5, fogColor[0], fogColor[1], fogColor[2], 1.0f);
        glUniform1i(6, RenderSystem.getShaderFogShape().getIndex());
        glUniform1f(7, fogIntensity);
        glUniform1f(8, fogDensity);
        glUniform1i(9, linearFog);

        //Do alpha blending

        glEnable(GL_BLEND);
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        AbstractRenderPipeline.transformBlitDepth(this.finalBlit, this.fb.getDepthTex().id, sourceFrameBuffer, viewport, new Matrix4f(viewport.vanillaProjection).mul(viewport.modelView));
        glDisable(GL_BLEND);
        //glBlitNamedFramebuffer(this.fbSSAO.id, sourceFrameBuffer, 0,0, viewport.width, viewport.height, 0,0, viewport.width, viewport.height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
    }

    @Override
    public void setupAndBindOpaque(Viewport<?> viewport) {
        this.fb.bind();
    }

    @Override
    public void setupAndBindTranslucent(Viewport<?> viewport) {
        glBindFramebuffer(GL_FRAMEBUFFER, this.fbSSAO.id);
    }

    @Override
    public void free() {
        this.finalBlit.delete();
        this.ssaoCompute.free();
        this.fb.free();
        this.fbSSAO.free();
        if (this.colourTex != null) {
            this.colourTex.free();
            this.colourSSAOTex.free();
        }
        super.free();
    }
}
