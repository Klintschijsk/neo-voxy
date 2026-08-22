package me.cortex.voxy.client.compat.littletiles;

import me.cortex.voxy.client.compat.LodPipelineHooks;
import me.cortex.voxy.client.compat.create.DistantMesh;
import me.cortex.voxy.client.compat.create.DistantMeshBuilder;
import me.cortex.voxy.client.compat.create.DistantShaders;
import me.cortex.voxy.client.compat.create.DistantVisibility;
import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.rendering.Viewport;
import me.cortex.voxy.common.config.section.SectionStorage;
import me.cortex.voxy.commonImpl.WorldIdentifier;
import me.cortex.voxy.commonImpl.compat.littletiles.LittleTilesCompat;
import me.cortex.voxy.commonImpl.compat.littletiles.LittleTilesStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.opengl.GL11C.GL_ALWAYS;
import static org.lwjgl.opengl.GL11C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_EQUAL;
import static org.lwjgl.opengl.GL11C.GL_KEEP;
import static org.lwjgl.opengl.GL11C.GL_REPLACE;
import static org.lwjgl.opengl.GL11C.GL_STENCIL_TEST;
import static org.lwjgl.opengl.GL11C.glDepthFunc;
import static org.lwjgl.opengl.GL11C.glDepthMask;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL11C.glStencilFunc;
import static org.lwjgl.opengl.GL11C.glStencilOp;
import static org.lwjgl.opengl.GL20C.glUniform2f;
import static org.lwjgl.opengl.GL20C.glUseProgram;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;

public final class LittleTilesDistantRenderer implements LodPipelineHooks.Renderer {
    private static final int CELLS = 8;
    private static final int MAX_UPLOADS_PER_TICK = 2;
    private static volatile LittleTilesDistantRenderer active;

    private final ConcurrentLinkedQueue<Update> updates = new ConcurrentLinkedQueue<>();
    private final Map<Long, Entry> sections = new HashMap<>();
    private final ArrayDeque<Long> bakeQueue = new ArrayDeque<>();
    private final HashSet<Long> queued = new HashSet<>();
    private SectionStorage storage;
    private boolean storageLoaded;
    private int lastScanX = Integer.MIN_VALUE, lastScanZ = Integer.MIN_VALUE;

    public LittleTilesDistantRenderer() {
        active = this;
    }

    public static void accept(SectionStorage storage, LittleTilesCompat.SectionSnapshot snapshot) {
        LittleTilesDistantRenderer renderer = active;
        if (renderer != null) renderer.updates.add(new Update(storage, snapshot));
    }

    @SubscribeEvent
    public void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var engine = WorldIdentifier.ofEngineNullable(mc.level);
        if (engine == null) return;
        if (this.storage != engine.storage) {
            clearMeshes();
            this.updates.clear();
            this.storage = engine.storage;
            this.storageLoaded = false;
        }
        if (!this.storageLoaded) {
            this.storageLoaded = true;
            for (var snapshot : LittleTilesStore.loadAll(this.storage)) {
                this.updates.add(new Update(this.storage, snapshot));
            }
        }

        var camera = mc.gameRenderer.getMainCamera().getPosition();
        double maxDistance = VoxyConfig.CONFIG.sectionRenderDistance * 32.0 * 16.0;
        Update update;
        int applied = 0;
        while (applied++ < 256 && (update = this.updates.poll()) != null) {
            if (update.storage != this.storage) continue;
            long key = LittleTilesStore.key(update.snapshot.sx(), update.snapshot.sy(), update.snapshot.sz());
            Entry old = this.sections.remove(key);
            if (old != null && old.mesh != null) old.mesh.free();
            this.queued.remove(key);
            if (update.snapshot.cells().isEmpty()) continue;
            this.sections.put(key, new Entry(update.snapshot, null));
            if (distanceSq(update.snapshot, camera.x, camera.y, camera.z) <= maxDistance * maxDistance
                    && this.queued.add(key)) this.bakeQueue.add(key);
        }

        int cx = ((int) Math.floor(camera.x)) >> 4;
        int cz = ((int) Math.floor(camera.z)) >> 4;
        if (this.lastScanX == Integer.MIN_VALUE || Math.abs(cx - this.lastScanX) >= 4 || Math.abs(cz - this.lastScanZ) >= 4) {
            this.lastScanX = cx;
            this.lastScanZ = cz;
            double farSq = (maxDistance + 256.0) * (maxDistance + 256.0);
            for (var item : this.sections.entrySet()) {
                var entry = item.getValue();
                double distanceSq = distanceSq(entry.snapshot, camera.x, camera.y, camera.z);
                if (entry.mesh == null && distanceSq <= maxDistance * maxDistance && this.queued.add(item.getKey())) {
                    this.bakeQueue.add(item.getKey());
                } else if (entry.mesh != null && distanceSq > farSq) {
                    entry.mesh.free();
                    entry.mesh = null;
                }
            }
        }

        int uploaded = 0;
        while (uploaded < MAX_UPLOADS_PER_TICK && !this.bakeQueue.isEmpty()) {
            long key = this.bakeQueue.removeFirst();
            this.queued.remove(key);
            Entry entry = this.sections.get(key);
            if (entry == null || entry.mesh != null) continue;
            if (distanceSq(entry.snapshot, camera.x, camera.y, camera.z) > maxDistance * maxDistance) continue;
            entry.mesh = bake(entry.snapshot);
            uploaded++;
        }
    }

    @SubscribeEvent
    public void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearMeshes();
        this.storage = null;
        this.storageLoaded = false;
        this.updates.clear();
    }

    @Override
    public void render(me.cortex.voxy.client.core.AbstractRenderPipeline pipeline, Viewport<?> viewport, int depthFunc) {
        if (this.sections.isEmpty() || !VoxyConfig.CONFIG.isRenderingEnabled()) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        pipeline.setupAndBindOpaque(viewport);

        int cameraSectionX = ((int) Math.floor(viewport.cameraX)) >> 4;
        int cameraSectionZ = ((int) Math.floor(viewport.cameraZ)) >> 4;
        int vanillaChunks = mc.options.getEffectiveRenderDistance();
        double maxDistance = VoxyConfig.CONFIG.sectionRenderDistance * 32.0 * 16.0;
        double maxDistanceSq = maxDistance * maxDistance;
        boolean bound = false;
        var transform = new Matrix4f();
        try {
            for (Entry entry : this.sections.values()) {
                var source = entry.snapshot;
                if (entry.mesh == null) continue;
                if (Math.abs(source.sx() - cameraSectionX) <= vanillaChunks
                        && Math.abs(source.sz() - cameraSectionZ) <= vanillaChunks) continue;
                double ox = source.sx() * 16.0, oy = source.sy() * 16.0, oz = source.sz() * 16.0;
                double dx = ox + 8.0 - viewport.cameraX;
                double dy = oy + 8.0 - viewport.cameraY;
                double dz = oz + 8.0 - viewport.cameraZ;
                if (dx * dx + dy * dy + dz * dz > maxDistanceSq) continue;
                if (!DistantVisibility.isBoxVisible(viewport, ox, oy, oz, ox + 16, oy + 16, oz + 16)) continue;
                if (!bound) {
                    DistantShaders.forPipeline(pipeline, true).bind();
                    DistantShaders.bindTextures();
                    glEnable(GL_DEPTH_TEST);
                    glDepthFunc(depthFunc);
                    glDepthMask(true);
                    glDisable(GL_CULL_FACE);
                    glEnable(GL_STENCIL_TEST);
                    glStencilFunc(GL_ALWAYS, 3, 0xFF);
                    glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
                    bound = true;
                }
                transform.set(viewport.MVP).translate((float) (ox - viewport.cameraX),
                        (float) (oy - viewport.cameraY), (float) (oz - viewport.cameraZ));
                DistantShaders.uploadTransform(transform);
                glUniform2f(4, 1.0f, 1.0f);
                entry.mesh.draw();
            }
            if (bound) {
                glBindVertexArray(0);
                glUseProgram(0);
            }
        } finally {
            if (bound) {
                glStencilFunc(GL_EQUAL, 1, 0x1);
                glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
            }
        }
    }

    private static DistantMesh bake(LittleTilesCompat.SectionSnapshot snapshot) {
        var occupied = new HashMap<Integer, LittleTilesCompat.Cell>(snapshot.cells().size() * 2);
        for (var cell : snapshot.cells()) occupied.put(cell.coordinate(), cell);
        var sprites = new TextureAtlasSprite[snapshot.materials().size()];
        var materialTints = new int[snapshot.materials().size()];
        var builder = new DistantMeshBuilder();
        try {
            var blockRenderer = Minecraft.getInstance().getBlockRenderer();
            for (int i = 0; i < sprites.length; i++) {
                BlockState state = snapshot.materials().get(i).state();
                sprites[i] = blockRenderer.getBlockModel(state).getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData.EMPTY);
                int tint = snapshot.materials().get(i).color() & 0xFFFFFF;
                int blockTint = Minecraft.getInstance().getBlockColors().getColor(state, Minecraft.getInstance().level,
                        new BlockPos(snapshot.sx() * 16 + 8, snapshot.sy() * 16 + 8, snapshot.sz() * 16 + 8), 0);
                materialTints[i] = blockTint == -1 ? tint : multiply(tint, blockTint);
            }
            for (var cell : snapshot.cells()) {
                int coordinate = cell.coordinate();
                int x = coordinate & 127, z = (coordinate >>> 7) & 127, y = (coordinate >>> 14) & 127;
                float x0 = x / 8.0f, y0 = y / 8.0f, z0 = z / 8.0f;
                float x1 = x0 + 0.125f, y1 = y0 + 0.125f, z1 = z0 + 0.125f;
                var sprite = sprites[cell.material()];
                int tint = materialTints[cell.material()];
                int sky = (cell.light() >>> 4) & 15, block = cell.light() & 15;
                if (x == 0 || !occupied.containsKey(coordinate - 1)) face(builder, Direction.WEST, x0,y0,z0,x1,y1,z1,sprite,sky,block,tint);
                if (x == 127 || !occupied.containsKey(coordinate + 1)) face(builder, Direction.EAST, x0,y0,z0,x1,y1,z1,sprite,sky,block,tint);
                if (y == 0 || !occupied.containsKey(coordinate - (1 << 14))) face(builder, Direction.DOWN, x0,y0,z0,x1,y1,z1,sprite,sky,block,tint);
                if (y == 127 || !occupied.containsKey(coordinate + (1 << 14))) face(builder, Direction.UP, x0,y0,z0,x1,y1,z1,sprite,sky,block,tint);
                if (z == 0 || !occupied.containsKey(coordinate - (1 << 7))) face(builder, Direction.NORTH, x0,y0,z0,x1,y1,z1,sprite,sky,block,tint);
                if (z == 127 || !occupied.containsKey(coordinate + (1 << 7))) face(builder, Direction.SOUTH, x0,y0,z0,x1,y1,z1,sprite,sky,block,tint);
            }
            return builder.build();
        } catch (Throwable t) {
            builder.discard();
            me.cortex.voxy.common.Logger.error("Baking LittleTiles LOD mesh", t);
            return null;
        }
    }

    private static void face(DistantMeshBuilder b, Direction d, float x0,float y0,float z0,float x1,float y1,float z1,
                             TextureAtlasSprite s, int sky, int block, int tint) {
        float u0=s.getU0(), u1=s.getU1(), v0=s.getV0(), v1=s.getV1();
        float shade = switch (d) { case DOWN -> 0.5f; case NORTH, SOUTH -> 0.8f; case WEST, EAST -> 0.6f; default -> 1.0f; };
        float[][] p = switch (d) {
            case DOWN -> new float[][]{{x0,y0,z1},{x1,y0,z1},{x1,y0,z0},{x0,y0,z0}};
            case UP -> new float[][]{{x0,y1,z0},{x1,y1,z0},{x1,y1,z1},{x0,y1,z1}};
            case NORTH -> new float[][]{{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z0}};
            case SOUTH -> new float[][]{{x0,y0,z1},{x0,y1,z1},{x1,y1,z1},{x1,y0,z1}};
            case WEST -> new float[][]{{x0,y0,z0},{x0,y1,z0},{x0,y1,z1},{x0,y0,z1}};
            case EAST -> new float[][]{{x1,y0,z1},{x1,y1,z1},{x1,y1,z0},{x1,y0,z0}};
        };
        float[][] uv={{u0,v1},{u0,v0},{u1,v0},{u1,v1}};
        for(int i=0;i<4;i++) b.rawVertex(p[i][0],p[i][1],p[i][2],uv[i][0],uv[i][1],sky,block,shade,d.ordinal(),tint);
    }

    private static int multiply(int a, int b) {
        return (((((a >> 16) & 255) * ((b >> 16) & 255) / 255) & 255) << 16)
                | (((((a >> 8) & 255) * ((b >> 8) & 255) / 255) & 255) << 8)
                | (((a & 255) * (b & 255) / 255) & 255);
    }

    private void clearMeshes() {
        for (var entry : this.sections.values()) if (entry.mesh != null) entry.mesh.free();
        this.sections.clear();
        this.bakeQueue.clear();
        this.queued.clear();
        this.lastScanX = this.lastScanZ = Integer.MIN_VALUE;
    }

    private static double distanceSq(LittleTilesCompat.SectionSnapshot snapshot, double x, double y, double z) {
        double dx = snapshot.sx() * 16.0 + 8.0 - x;
        double dy = snapshot.sy() * 16.0 + 8.0 - y;
        double dz = snapshot.sz() * 16.0 + 8.0 - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private record Update(SectionStorage storage, LittleTilesCompat.SectionSnapshot snapshot) {}
    private static final class Entry {
        final LittleTilesCompat.SectionSnapshot snapshot;
        DistantMesh mesh;
        Entry(LittleTilesCompat.SectionSnapshot snapshot, DistantMesh mesh) {
            this.snapshot = snapshot;
            this.mesh = mesh;
        }
    }
}
