package com.l7cor1s.viktoria;

import com.l7cor1s.viktoria.client.CompanionRenderer;
import com.l7cor1s.viktoria.entity.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = ViktoriaMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ViktoriaMod.MODID, value = Dist.CLIENT)
public class ViktoriaModClient {
    public ViktoriaModClient(ModContainer container) {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ViktoriaMod.LOGGER.info("Клиент Виктории готов");
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.COMPANION.get(), CompanionRenderer::new);
    }
}
