package me.cortex.voxy.client.core.rendering;

import me.cortex.voxy.client.core.AbstractRenderPipeline;
import me.cortex.voxy.client.core.gl.GlBuffer;
import me.cortex.voxy.client.core.gl.GlVertexArray;
import me.cortex.voxy.client.core.gl.shader.AutoBindingShader;
import me.cortex.voxy.client.core.gl.shader.Shader;
import me.cortex.voxy.client.core.gl.shader.ShaderLoader;
import me.cortex.voxy.client.core.gl.shader.ShaderType;
import me.cortex.voxy.client.core.rendering.util.SharedIndexBuffer;
import me.cortex.voxy.client.core.rendering.util.UploadStream;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryUtil;

import java.util.Arrays;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL31.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL42.glDrawElementsInstancedBaseInstance;

/** Masks only the chunk sections Embeddium actually renders. */
public class ChunkBoundRenderer {
    private static final int INITIAL_SECTION_COUNT = 1 << 12;

    private GlBuffer sectionPosBuffer = new GlBuffer(INITIAL_SECTION_COUNT * 8L);
    private final GlBuffer uniformBuffer = new GlBuffer(128);
    private final Shader rasterShader;
    private final AbstractRenderPipeline pipeline;

    private int[] visibleSections = new int[INITIAL_SECTION_COUNT * 2];
    private int count;
    private boolean changed;
    private int lastRenderedSectionCount;

    public ChunkBoundRenderer(AbstractRenderPipeline pipeline) {
        this.pipeline = pipeline;

        String vert = ShaderLoader.parse("voxy:chunkoutline/outline.vsh");
        String taa = pipeline.taaFunction("getTAA");
        if (taa != null) {
            vert = vert + "\n\n\n" + taa;
        }
        this.rasterShader = Shader.makeAuto()
                .addSource(ShaderType.VERTEX, vert)
                .defineIf("TAA", taa != null)
                .add(ShaderType.FRAGMENT, "voxy:chunkoutline/outline.fsh")
                .compile()
                .ubo(0, this.uniformBuffer)
                .ssbo(1, this.sectionPosBuffer);
    }

    public void put(long pos) {
        if (this.count >= this.visibleSections.length - 1) {
            this.visibleSections = Arrays.copyOf(this.visibleSections,
                    Math.max(this.visibleSections.length + 2, (int) (this.visibleSections.length * 1.25f)));
        }
        this.visibleSections[this.count++] = (int) pos;
        this.visibleSections[this.count++] = (int) (pos >>> 32);
    }

    public void reset() {
        this.count = 0;
        this.changed = true;
    }

    public int getLastRenderedSectionCount() {
        return this.lastRenderedSectionCount;
    }

    public void render(Viewport<?> viewport) {
        viewport.depthBoundingBuffer.clear(0);

        int sectionCount = this.count >> 1;
        this.lastRenderedSectionCount = sectionCount;
        if (sectionCount == 0) {
            return;
        }

        if (this.changed) {
            long byteCount = this.count * 4L;
            if (byteCount > this.sectionPosBuffer.size()) {
                this.sectionPosBuffer.free();
                this.sectionPosBuffer = new GlBuffer((long) Math.ceil(byteCount * 1.25));
                ((AutoBindingShader) this.rasterShader).ssbo(1, this.sectionPosBuffer);
            }

            long upload = UploadStream.INSTANCE.upload(this.sectionPosBuffer, 0, byteCount);
            for (int i = 0; i < this.count; i++) {
                MemoryUtil.memPutInt(upload + i * 4L, this.visibleSections[i]);
            }
            UploadStream.INSTANCE.commit();
            this.changed = false;
        }

        long ptr = UploadStream.INSTANCE.upload(this.uniformBuffer, 0, 96);
        long matPtr = ptr;
        ptr += 4 * 4 * 4;

        int bx = (int) Math.floor(viewport.cameraX);
        int by = (int) Math.floor(viewport.cameraY);
        int bz = (int) Math.floor(viewport.cameraZ);
        new Vector3i(bx, by, bz).getToAddress(ptr);
        ptr += 4 * 4;

        var remainder = new Vector3f(
                (float) (viewport.cameraX - bx),
                (float) (viewport.cameraY - by),
                (float) (viewport.cameraZ - bz));
        remainder.getToAddress(ptr);
        viewport.MVP.translate(remainder.negate(), new Matrix4f()).getToAddress(matPtr);
        UploadStream.INSTANCE.commit();

        glFrontFace(GL_CW);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_GREATER);

        glBindVertexArray(GlVertexArray.STATIC_VAO);
        viewport.depthBoundingBuffer.bind();
        this.rasterShader.bind();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, SharedIndexBuffer.INSTANCE_BB_BYTE.id());
        this.pipeline.bindUniforms();

        if (sectionCount >= 32) {
            glDrawElementsInstanced(GL_TRIANGLES, 6 * 2 * 3 * 32, GL_UNSIGNED_BYTE, 0, sectionCount / 32);
        }
        if (sectionCount % 32 != 0) {
            glDrawElementsInstancedBaseInstance(GL_TRIANGLES, 6 * 2 * 3 * (sectionCount % 32),
                    GL_UNSIGNED_BYTE, 0, 1, (sectionCount / 32) * 32);
        }

        glFrontFace(GL_CCW);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    public void free() {
        this.rasterShader.free();
        this.uniformBuffer.free();
        this.sectionPosBuffer.free();
    }
}
