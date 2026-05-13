package net.tropimon.safaribeacon.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin vide requis par la configuration Mixin de Fabric.
 * Le rendu est géré via WorldRenderEvents dans SafariBeaconClient.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    // Pas d'injection nécessaire ici
}
