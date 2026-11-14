package com.l7cor1s.viktoria.quest;

import com.l7cor1s.viktoria.character.CompanionCharacter;
import com.l7cor1s.viktoria.entity.CompanionEntity;
import com.l7cor1s.viktoria.relationship.CompanionState;
import com.l7cor1s.viktoria.relationship.MoodType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

public final class QuestProgress {
    public static final int STAGE_LOCKED = 0;
    public static final int STAGE_ACTIVE = 1;
    public static final int STAGE_DONE = 2;

    private QuestProgress() {
    }

    public static void startNext(ServerPlayer player, CompanionEntity companion, CompanionState state) {
        CompanionCharacter character = companion.character();
        for (QuestDefinition quest : character.quests()) {
            int stage = state.questStage(quest.id());
            if (stage == STAGE_LOCKED && state.affinity() >= quest.minAffinity()) {
                state.setQuestStage(quest.id(), STAGE_ACTIVE);
                state.setQuestCounter(quest.id(), 0);
                state.addAffinity(2);
                state.setMood(MoodType.JOY);
                player.displayClientMessage(Component.translatable("viktoria.quest.started",
                        Component.translatable(quest.langKey())), false);
                player.displayClientMessage(Component.translatable(objectiveHint(quest)), false);
                return;
            }
            if (stage == STAGE_ACTIVE) {
                player.displayClientMessage(Component.translatable("viktoria.quest.already_active",
                        Component.translatable(quest.langKey())), true);
                showStatus(player, companion, state);
                return;
            }
        }
        player.displayClientMessage(Component.translatable("viktoria.quest.none"), true);
    }

    public static void showStatus(ServerPlayer player, CompanionEntity companion, CompanionState state) {
        for (QuestDefinition quest : companion.character().quests()) {
            int stage = state.questStage(quest.id());
            if (stage == STAGE_ACTIVE) {
                player.displayClientMessage(Component.translatable("viktoria.quest.status_active",
                        Component.translatable(quest.langKey()),
                        state.questCounter(quest.id()),
                        quest.targetCount()), false);
                return;
            }
        }
        player.displayClientMessage(Component.translatable("viktoria.quest.status_none"), true);
    }

    public static void onGift(ServerPlayer player, CompanionEntity companion, CompanionState state, ItemStack stack) {
        for (QuestDefinition quest : companion.character().quests()) {
            if (state.questStage(quest.id()) != STAGE_ACTIVE) {
                continue;
            }
            if (quest.objective() == QuestObjective.GIVE_FLOWERS && stack.is(ItemTags.FLOWERS)) {
                int next = state.questCounter(quest.id()) + 1;
                state.setQuestCounter(quest.id(), next);
                player.displayClientMessage(Component.translatable("viktoria.quest.progress",
                        Component.translatable(quest.langKey()), next, quest.targetCount()), true);
                tryComplete(player, companion, state, quest);
                return;
            }
        }
    }

    public static void onHomeClaimed(ServerPlayer player, CompanionEntity companion, CompanionState state) {
        for (QuestDefinition quest : companion.character().quests()) {
            if (state.questStage(quest.id()) != STAGE_ACTIVE) {
                continue;
            }
            if (quest.objective() == QuestObjective.CLAIM_HOME) {
                state.setQuestCounter(quest.id(), quest.targetCount());
                tryComplete(player, companion, state, quest);
                return;
            }
        }
    }

    public static void onFollowNightTick(ServerPlayer player, CompanionEntity companion, CompanionState state) {
        for (QuestDefinition quest : companion.character().quests()) {
            if (state.questStage(quest.id()) != STAGE_ACTIVE) {
                continue;
            }
            if (quest.objective() == QuestObjective.FOLLOW_AT_NIGHT && state.following()) {
                int next = state.questCounter(quest.id()) + 1;
                state.setQuestCounter(quest.id(), next);
                if (next == 1 || next % 100 == 0 || next >= quest.targetCount()) {
                    player.displayClientMessage(Component.translatable("viktoria.quest.progress",
                            Component.translatable(quest.langKey()), next, quest.targetCount()), true);
                }
                tryComplete(player, companion, state, quest);
                return;
            }
        }
    }

    private static void tryComplete(ServerPlayer player, CompanionEntity companion, CompanionState state, QuestDefinition quest) {
        if (state.questCounter(quest.id()) < quest.targetCount()) {
            return;
        }
        state.setQuestStage(quest.id(), STAGE_DONE);
        state.addAffinity(quest.rewardAffinity());
        state.setMood(MoodType.JOY);
        player.displayClientMessage(Component.translatable("viktoria.quest.completed",
                Component.translatable(quest.langKey()), quest.rewardAffinity()), false);
    }

    private static String objectiveHint(QuestDefinition quest) {
        return switch (quest.objective()) {
            case GIVE_FLOWERS -> "viktoria.quest.hint.flowers";
            case CLAIM_HOME -> "viktoria.quest.hint.home";
            case FOLLOW_AT_NIGHT -> "viktoria.quest.hint.walk";
        };
    }
}
