package com.l7cor1s.viktoria.client;

import com.l7cor1s.viktoria.client.screen.DialogueScreen;
import com.l7cor1s.viktoria.network.OpenDialoguePayload;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void openDialogue(OpenDialoguePayload payload) {
        Minecraft.getInstance().setScreen(new DialogueScreen(
                payload.entityId(),
                payload.characterId(),
                payload.nodeId(),
                payload.affinity(),
                payload.mood(),
                payload.tier()
        ));
    }
}
