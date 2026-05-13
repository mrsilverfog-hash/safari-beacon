package net.tropimon.safaribeacon.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
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
import java.util.Optional;

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

    private static final int SEARCH_RADIUS = 48;
    private static final int BEAM_HEIGHT = 256;
    private static final float BEAM_INNER_RADIUS = 0.12f;
    private static final float BEAM_OUTER_RADIUS = 0.25f;

    public static void onWorldRenderLast(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        if (world == null || client.player == null) return;

        List<BlockPos> safariBlocks = findNearbyBlocks(world, client.player.getBlockPos());
        if (safariBlocks.isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();

        float tickDelta = context.tickCounter().getTickDelta(true);
        long time = world.getTime();
        float angle = ((time % 360) + tickDelta) * 2.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        Tessellator tessellator = Tessellator.getInstance();

        for (BlockPos pos : safariBlocks) {
            matrices.push();
            matrices.translate(
                pos.getX() + 0.5 - camPos.x,
                pos.getY() + 1.0 - camPos.y,
                pos.getZ() + 0.5 - camPos.z
            );
            renderBeam(matrices, tessellator, angle);
            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void renderBeam(MatrixStack matrices, Tessellator tessellator, float angle) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
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
        // Récupérer la propriété "available" directement depuis le bloc
        Collection<Property<?>> properties = state.getProperties();
        for (Property<?> prop : properties) {
            if (prop.getName().equals("available")) {
                // Lire la valeur comme Comparable puis comparer à true
                Comparable<?> value = state.get((Property) prop);
                return Boolean.TRUE.equals(value);
            }
        }
        // Si propriété introuvable, afficher quand même le faisceau
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
