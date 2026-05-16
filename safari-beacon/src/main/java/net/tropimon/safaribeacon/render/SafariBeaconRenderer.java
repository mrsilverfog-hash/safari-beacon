package net.tropimon.safaribeacon.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SafariBeaconRenderer {

    private static final String MOD_ID = "tropifurnitures";
    private static final String[] BLOCK_NAMES = {
        "suspicious_safari_gravel",
        "suspicious_safari_sand"
    };

    private static final float BEAM_RED   = 1.0f;
    private static final float BEAM_GREEN = 0.10f;
    private static final float BEAM_BLUE  = 0.08f;
    private static final float BEAM_ALPHA_INNER = 0.92f;
    private static final float BEAM_ALPHA_OUTER  = 0.65f;

    private static final int SEARCH_RADIUS = 64;
    private static final int BEAM_HEIGHT = 256;
    private static final float BEAM_INNER_RADIUS = 0.12f;
    private static final float BEAM_OUTER_RADIUS = 0.25f;

    // Thread séparé pour le scan — ne bloque plus le jeu
    private static final ScheduledExecutorService SCANNER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "safari-beacon-scanner");
        t.setDaemon(true);
        return t;
    });

    // Liste thread-safe mise à jour par le scanner
    private static final AtomicReference<List<BlockPos>> cachedBlocks = new AtomicReference<>(new ArrayList<>());
    private static volatile boolean scanScheduled = false;

    public static void resetCache() {
        cachedBlocks.set(new ArrayList<>());
        scanScheduled = false;
    }

    public static void onWorldRenderLast(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        if (world == null || client.player == null) return;

        // Lancer un scan en arrière-plan toutes les 2 secondes
        if (!scanScheduled) {
            scanScheduled = true;
            BlockPos playerPos = client.player.getBlockPos();
            SCANNER.scheduleAtFixedRate(() -> {
                try {
                    List<BlockPos> found = findNearbyBlocks(world, playerPos);
                    cachedBlocks.set(found);
                } catch (Exception ignored) {}
            }, 0, 2, TimeUnit.SECONDS);
        }

        List<BlockPos> blocks = cachedBlocks.get();
        if (blocks.isEmpty()) return;

        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        long currentTick = world.getTime();
        float tickDelta = context.tickCounter().getTickDelta(true);
        float angle = ((currentTick % 360) + tickDelta) * 2.0f;

        Matrix4f viewMatrix = context.matrixStack().peek().getPositionMatrix();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();

        Tessellator tessellator = Tessellator.getInstance();

        for (BlockPos pos : blocks) {
            Matrix4f modelMatrix = new Matrix4f(viewMatrix);
            modelMatrix.translate(
                (float)(pos.getX() + 0.5 - camPos.x),
                (float)(pos.getY() + 1.0 - camPos.y),
                (float)(pos.getZ() + 0.5 - camPos.z)
            );
            renderBeam(tessellator, modelMatrix, angle);
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void renderBeam(Tessellator tessellator, Matrix4f matrix, float angle) {
        int sides = 8;
        drawBeamLayer(tessellator, matrix, BEAM_INNER_RADIUS, BEAM_HEIGHT, angle,
            BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA_INNER, sides);
        drawBeamLayer(tessellator, matrix, BEAM_OUTER_RADIUS, BEAM_HEIGHT, angle * 0.7f,
            BEAM_RED, BEAM_GREEN * 0.5f, BEAM_BLUE * 0.5f, BEAM_ALPHA_OUTER, sides);
    }

    private static void drawBeamLayer(Tessellator tessellator, Matrix4f matrix,
                                       float radius, float height, float rotAngle,
                                       float r, float g, float b, float a, int sides) {
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        double angleOffset = Math.toRadians(rotAngle);
        for (int i = 0; i < sides; i++) {
            double angle1 = angleOffset + (2 * Math.PI * i / sides);
            double angle2 = angleOffset + (2 * Math.PI * (i + 1) / sides);
            float x1 = (float)(Math.cos(angle1) * radius);
            float z1 = (float)(Math.sin(angle1) * radius);
            float x2 = (float)(Math.cos(angle2) * radius);
            float z2 = (float)(Math.sin(angle2) * radius);
            buffer.vertex(matrix, x1, 0,      z1).color(r, g, b, a);
            buffer.vertex(matrix, x2, 0,      z2).color(r, g, b, a);
            buffer.vertex(matrix, x2, height, z2).color(r, g, b, 0f);
            buffer.vertex(matrix, x1, height, z1).color(r, g, b, 0f);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    @SuppressWarnings("unchecked")
    private static boolean isAvailable(BlockState state) {
        Collection<Property<?>> properties = state.getProperties();
        for (Property<?> prop : properties) {
            if (prop.getName().equals("available")) {
                Comparable<?> value = state.get((Property) prop);
                return Boolean.TRUE.equals(value);
            }
        }
        return true;
    }

    private static List<BlockPos> findNearbyBlocks(World world, BlockPos center) {
        List<BlockPos> result = new ArrayList<>();
        try {
            BlockPos.iterate(
                center.add(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS),
                center.add(SEARCH_RADIUS,  SEARCH_RADIUS,  SEARCH_RADIUS)
            ).forEach(pos -> {
                BlockState state = world.getBlockState(pos);
                Identifier id = Registries.BLOCK.getId(state.getBlock());
                if (id.getNamespace().equals(MOD_ID)) {
                    for (String name : BLOCK_NAMES) {
                        if (id.getPath().equals(name)) {
                            if (isAvailable(state)) {
                                result.add(pos.toImmutable());
                            }
                            break;
                        }
                    }
                }
            });
        } catch (Exception ignored) {}
        return result;
    }
}
