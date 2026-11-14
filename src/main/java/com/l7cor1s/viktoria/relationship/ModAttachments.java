package com.l7cor1s.viktoria.relationship;

import com.l7cor1s.viktoria.ViktoriaMod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ViktoriaMod.MODID);

    public static final Supplier<AttachmentType<CompanionPlayerData>> COMPANION_DATA = ATTACHMENT_TYPES.register(
            "companion_data",
            () -> AttachmentType.<CompanionPlayerData>builder(() -> new CompanionPlayerData())
                    .serialize(CompanionPlayerData.CODEC)
                    .copyOnDeath()
                    .build()
    );

    private ModAttachments() {
    }
}
