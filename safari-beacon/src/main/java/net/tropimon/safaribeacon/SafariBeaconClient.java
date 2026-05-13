package net.tropimon.safaribeacon;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.tropimon.safaribeacon.render.SafariBeaconRenderer;

public class SafariBeaconClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Enregistrer le renderer de faisceau après le rendu du monde
        WorldRenderEvents.LAST.register(SafariBeaconRenderer::onWorldRenderLast);
    }
}
