package com.l7cor1s.viktoria.event;

import com.l7cor1s.viktoria.character.ViktoriaCharacter;
import com.l7cor1s.viktoria.entity.CompanionEntity;
import com.l7cor1s.viktoria.relationship.CompanionState;
import com.l7cor1s.viktoria.relationship.ModAttachments;
import com.l7cor1s.viktoria.relationship.MoodType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Reactions to player actions: hurt nearby companion, login restore follow, etc. */
public final class ModEvents {
    private ModEvents() {
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        // copyOnDeath handles attachment; keep hook for future custom merge.
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (!(victim instanceof CompanionEntity companion)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!companion.getCharacterId().equals(ViktoriaCharacter.ID)) {
            return;
        }
        CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get()).getOrCreate(companion.getCharacterId());
        state.addAffinity(-10);
        state.setMood(MoodType.IRRITATION);
        state.setFollowing(false);
        companion.setFollowing(false);
        companion.setAnimState("talk");
    }
}
