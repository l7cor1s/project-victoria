package com.l7cor1s.viktoria.network;

import com.l7cor1s.viktoria.ViktoriaMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ViktoriaMod.MODID).versioned("1");
        registrar.playToClient(OpenDialoguePayload.TYPE, OpenDialoguePayload.STREAM_CODEC, OpenDialoguePayload::handle);
        registrar.playToServer(DialogueChoicePayload.TYPE, DialogueChoicePayload.STREAM_CODEC, DialogueChoicePayload::handle);
    }
}
