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
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    private static List<BlockPos> cachedBlocks = new ArrayList<>();
    private static long lastScanTick = -1;
    private static final int SCAN_INTERVAL = 40;

    public static void resetCache() {
        cachedBlocks = new ArrayList<>();
        lastScanTick = -1;
    }

    public static void onWorldRenderLast(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        if (world == null || client.player == null) return;

        long currentTick = world.getTime();

        if (currentTick - lastScanTick >= SCAN_INTERVAL) {
            cachedBlocks = findNearbyBlocks(world, client.player.getBlockPos());
            lastScanTick = currentTick;
        }

        if (cachedBlocks.isEmpty()) return;

        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        float tickDelta = context.tickCounter().getTickDelta(true);
        float angle = ((currentTick % 360) + tickDelta) * 2.0f;

        // Récupérer les matrices de projection correctes
        Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();
        Matrix4f viewMatrix = context.matrixStack().peek().getPositionMatrix();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();

        Tessellator tessellator = Tessellator.getInstance();

        for (BlockPos pos : cachedBlocks) {
            // Construire la matrice de translation depuis la caméra
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
        return result;
    }
}
