package com.l7cor1s.viktoria.neoforge;

import com.l7cor1s.viktoria.ViktoriaMod;
import com.l7cor1s.viktoria.client.VikaClient;
import com.l7cor1s.viktoria.client.gui.VikaInventoryScreen;
import com.l7cor1s.viktoria.client.gui.NeedsHud;
import com.l7cor1s.viktoria.client.render.VikaRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge client entry point. Only loaded on the physical client.
 *
 * @author vyrriox
 */
final class VikaNeoForgeClient {
    private VikaNeoForgeClient() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(() -> {
            VikaClient.init();
            VikaClient.afterRegistries();
        }));
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class,
                event -> event.registerEntityRenderer(VikaNeoForge.LAURA.get(), VikaRenderer::new));
        modBus.addListener(RegisterMenuScreensEvent.class,
                event -> event.register(VikaNeoForge.INVENTORY_MENU.get(), VikaInventoryScreen::new));
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
            for (KeyMapping key : VikaClient.keyMappings()) {
                event.register(key);
            }
        });
        modBus.addListener(RegisterClientReloadListenersEvent.class,
                event -> event.registerReloadListener((ResourceManagerReloadListener) manager -> VikaClient.onResourceReload()));
        modBus.addListener(RegisterGuiLayersEvent.class,
                event -> event.registerAboveAll(ViktoriaMod.id("needs_hud"), (graphics, delta) -> NeedsHud.render(graphics)));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> VikaClient.tick());
    }
}
