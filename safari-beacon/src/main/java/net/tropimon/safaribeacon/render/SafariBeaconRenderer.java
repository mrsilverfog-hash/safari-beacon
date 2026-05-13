package net.tropimon.safaribeacon.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class SafariBeaconRenderer {

    // Identifiants des blocs TropiFurnitures à surveiller
    private static final String MOD_ID = "tropifurnitures";
    private static final String[] BLOCK_NAMES = {
        "suspicious_safari_gravel",
        "suspicious_safari_sand"
    };

    // Couleur du faisceau : rouge vif (comme la croix)
    private static final float BEAM_RED   = 1.0f;
    private static final float BEAM_GREEN = 0.15f;
    private static final float BEAM_BLUE  = 0.10f;
    private static final float BEAM_ALPHA = 0.6f;

    // Rayon de recherche autour du joueur (en blocs)
    private static final int SEARCH_RADIUS = 48;

    // Hauteur du faisceau (monte jusqu'en haut du monde)
    private static final int BEAM_HEIGHT = 256;

    // Rayon intérieur et extérieur du faisceau
    private static final float BEAM_INNER_RADIUS = 0.1f;
    private static final float BEAM_OUTER_RADIUS = 0.2f;

    public static void onWorldRenderLast(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        if (world == null || client.player == null) return;

        List<BlockPos> safariBlocks = findNearbyBlocks(world, client.player.getBlockPos());
        if (safariBlocks.isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();

        // Récupérer le temps pour l'animation de rotation
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
            // Décaler par rapport à la caméra
            matrices.translate(
                pos.getX() + 0.5 - camPos.x,
                pos.getY() + 1.0 - camPos.y,
                pos.getZ() + 0.5 - camPos.z
            );

            renderBeam(matrices, tessellator, angle, tickDelta);
            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void renderBeam(MatrixStack matrices, Tessellator tessellator, float angle, float tickDelta) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Dessiner un tube octogonal (8 faces)
        int sides = 8;
        float innerR = BEAM_INNER_RADIUS;
        float outerR = BEAM_OUTER_RADIUS;
        float height = BEAM_HEIGHT;

        // Couche intérieure (plus opaque)
        drawBeamLayer(tessellator, matrix, innerR, height, angle,
            BEAM_RED, BEAM_GREEN, BEAM_BLUE, BEAM_ALPHA, sides);

        // Couche extérieure (plus transparente, halo)
        drawBeamLayer(tessellator, matrix, outerR, height, angle * 0.7f,
            BEAM_RED, BEAM_GREEN * 0.5f, BEAM_BLUE * 0.5f, BEAM_ALPHA * 0.4f, sides);
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

            // Face de la colonne (bas → haut)
            buffer.vertex(matrix, x1, 0,      z1).color(r, g, b, a);
            buffer.vertex(matrix, x2, 0,      z2).color(r, g, b, a);
            buffer.vertex(matrix, x2, height, z2).color(r, g, b, 0f); // fondu vers le haut
            buffer.vertex(matrix, x1, height, z1).color(r, g, b, 0f);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
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
                        result.add(pos.toImmutable());
                        break;
                    }
                }
            }
        });

        return result;
    }
}
