package com.l7cor1s.viktoria.network;

import com.l7cor1s.viktoria.ViktoriaMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenDialoguePayload(
        int entityId,
        String characterId,
        String nodeId,
        int affinity,
        String mood,
        String tier
) implements CustomPacketPayload {
    public static final Type<OpenDialoguePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ViktoriaMod.MODID, "open_dialogue"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDialoguePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenDialoguePayload::entityId,
            ByteBufCodecs.STRING_UTF8, OpenDialoguePayload::characterId,
            ByteBufCodecs.STRING_UTF8, OpenDialoguePayload::nodeId,
            ByteBufCodecs.VAR_INT, OpenDialoguePayload::affinity,
            ByteBufCodecs.STRING_UTF8, OpenDialoguePayload::mood,
            ByteBufCodecs.STRING_UTF8, OpenDialoguePayload::tier,
            OpenDialoguePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenDialoguePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                ClientProxy.openDialogue(payload);
            }
        });
    }

    /** Tiny indirection so the server classpath never touches client GUI classes. */
    private static final class ClientProxy {
        private static void openDialogue(OpenDialoguePayload payload) {
            com.l7cor1s.viktoria.client.ClientPacketHandlers.openDialogue(payload);
        }
    }
}
