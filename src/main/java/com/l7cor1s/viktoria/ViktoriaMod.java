package com.l7cor1s.viktoria;

import com.l7cor1s.viktoria.character.CharacterRegistry;
import com.l7cor1s.viktoria.character.ViktoriaCharacter;
import com.l7cor1s.viktoria.command.ViktoriaCommands;
import com.l7cor1s.viktoria.entity.ModEntities;
import com.l7cor1s.viktoria.network.ModNetworking;
import com.l7cor1s.viktoria.relationship.ModAttachments;
import com.l7cor1s.viktoria.spawn.McaIntegration;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(ViktoriaMod.MODID)
public class ViktoriaMod {
    public static final String MODID = "viktoria";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ViktoriaMod(IEventBus modEventBus, ModContainer modContainer) {
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModNetworking::register);
        modEventBus.addListener(ModEntities::registerAttributes);

        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.register(com.l7cor1s.viktoria.event.ModEvents.class);
        NeoForge.EVENT_BUS.register(com.l7cor1s.viktoria.spawn.VillageSpawnHandler.class);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CharacterRegistry.register(new ViktoriaCharacter());
            McaIntegration.bootstrap();
            LOGGER.info("Viktoria companion framework ready. Characters: {}", CharacterRegistry.ids());
        });
    }

    private void registerCommands(RegisterCommandsEvent event) {
        ViktoriaCommands.register(event.getDispatcher());
    }
}
