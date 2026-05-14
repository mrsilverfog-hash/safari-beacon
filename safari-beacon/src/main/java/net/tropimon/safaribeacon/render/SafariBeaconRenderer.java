package net.tropimon.safaribeacon;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.tropimon.safaribeacon.render.SafariBeaconRenderer;

public class SafariBeaconClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.LAST.register(SafariBeaconRenderer::onWorldRenderLast);

        // Réinitialiser le cache à chaque connexion/déconnexion
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            SafariBeaconRenderer.resetCache();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SafariBeaconRenderer.resetCache();
        });
    }
}
