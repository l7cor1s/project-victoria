package com.l7cor1s.viktoria.quest;

import com.l7cor1s.viktoria.character.CompanionCharacter;
import com.l7cor1s.viktoria.entity.CompanionEntity;
import com.l7cor1s.viktoria.relationship.CompanionState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class QuestProgress {
    private QuestProgress() {
    }

    public static void startNext(ServerPlayer player, CompanionEntity companion, CompanionState state) {
        CompanionCharacter character = companion.character();
        for (QuestDefinition quest : character.quests()) {
            int stage = state.questStage(quest.id());
            if (stage == 0 && state.affinity() >= quest.minAffinity()) {
                state.setQuestStage(quest.id(), 1);
                state.addAffinity(2);
                player.displayClientMessage(Component.translatable("viktoria.quest.started",
                        Component.translatable(quest.langKey())), false);
                return;
            }
            if (stage == 1) {
                // Simple auto-complete hook for the SP slice; expand with real objectives later.
                state.setQuestStage(quest.id(), 2);
                state.addAffinity(quest.rewardAffinity());
                player.displayClientMessage(Component.translatable("viktoria.quest.completed",
                        Component.translatable(quest.langKey()), quest.rewardAffinity()), false);
                return;
            }
        }
        player.displayClientMessage(Component.translatable("viktoria.quest.none"), true);
    }
}
