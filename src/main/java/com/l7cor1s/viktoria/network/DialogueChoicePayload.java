package com.l7cor1s.viktoria.network;

import com.l7cor1s.viktoria.ViktoriaMod;
import com.l7cor1s.viktoria.dialogue.DialogueManager;
import com.l7cor1s.viktoria.entity.CompanionEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DialogueChoicePayload(
        int entityId,
        String nodeId,
        String choiceId
) implements CustomPacketPayload {
    public static final Type<DialogueChoicePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ViktoriaMod.MODID, "dialogue_choice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DialogueChoicePayload::entityId,
            ByteBufCodecs.STRING_UTF8, DialogueChoicePayload::nodeId,
            ByteBufCodecs.STRING_UTF8, DialogueChoicePayload::choiceId,
            DialogueChoicePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DialogueChoicePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.level().getEntity(payload.entityId());
            if (entity instanceof CompanionEntity companion) {
                DialogueManager.applyChoice(player, companion, payload.nodeId(), payload.choiceId());
            }
        });
    }
}
