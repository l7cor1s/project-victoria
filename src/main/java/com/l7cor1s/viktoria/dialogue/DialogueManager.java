package com.l7cor1s.viktoria.dialogue;

import com.l7cor1s.viktoria.entity.CompanionEntity;
import com.l7cor1s.viktoria.home.HomeManager;
import com.l7cor1s.viktoria.intimate.IntimateHooks;
import com.l7cor1s.viktoria.network.OpenDialoguePayload;
import com.l7cor1s.viktoria.quest.QuestProgress;
import com.l7cor1s.viktoria.relationship.CompanionState;
import com.l7cor1s.viktoria.relationship.ModAttachments;
import com.l7cor1s.viktoria.relationship.MoodType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DialogueManager {
    private DialogueManager() {
    }

    public static void open(ServerPlayer player, CompanionEntity companion, String nodeId) {
        String characterId = companion.getCharacterId();
        DialogueNode node = DialogueTrees.get(characterId, nodeId).orElse(null);
        if (node == null) {
            return;
        }
        CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get()).getOrCreate(characterId);
        if (state.affinity() < node.minAffinity()) {
            player.displayClientMessage(Component.translatable("viktoria.dialogue.locked_affinity",
                    companion.character().displayName()), true);
            return;
        }
        if (node.requiredMood() != null && !node.requiredMood().equalsIgnoreCase(state.mood().getSerializedName())) {
            player.displayClientMessage(Component.translatable("viktoria.dialogue.locked_mood",
                    companion.character().displayName()), true);
            return;
        }
        PacketDistributor.sendToPlayer(player, new OpenDialoguePayload(
                companion.getId(),
                characterId,
                node.id(),
                state.affinity(),
                state.mood().getSerializedName(),
                state.tier().name()
        ));
    }

    public static void applyChoice(ServerPlayer player, CompanionEntity companion, String nodeId, String choiceId) {
        String characterId = companion.getCharacterId();
        DialogueNode node = DialogueTrees.get(characterId, nodeId).orElse(null);
        if (node == null) {
            return;
        }
        CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get()).getOrCreate(characterId);
        DialogueChoice choice = node.visibleChoices(state.affinity(), state.mood()).stream()
                .filter(c -> c.id().equals(choiceId))
                .findFirst()
                .orElse(null);
        if (choice == null) {
            return;
        }

        state.touch(player.level().getGameTime());
        if (choice.affinityDelta() != 0) {
            state.addAffinity(choice.affinityDelta());
        }
        if (choice.mood() != null) {
            state.setMood(MoodType.byName(choice.mood()));
        }

        String action = choice.action();
        if (action != null) {
            switch (action) {
                case "close" -> {
                    return;
                }
                case "toggle_follow" -> {
                    boolean next = !state.following();
                    state.setFollowing(next);
                    companion.setFollowing(next);
                    player.displayClientMessage(Component.translatable(
                            next ? "viktoria.follow.on" : "viktoria.follow.off",
                            companion.character().displayName()), true);
                }
                case "show_status" -> player.displayClientMessage(Component.translatable(
                        "viktoria.status.line",
                        companion.character().displayName(),
                        state.affinity(),
                        state.tier().label(),
                        state.mood().label()), false);
                case "claim_home" -> HomeManager.claimHome(player, companion, state);
                case "live_together" -> HomeManager.setLivesTogether(player, companion, state, true);
                case "start_quest" -> QuestProgress.startNext(player, companion, state);
                case "quest_status" -> QuestProgress.showStatus(player, companion, state);
                case "intimate_consent" -> {
                    IntimateHooks hooks = companion.character().intimateHooks();
                    if (hooks.canOffer(state)) {
                        state.setIntimateConsent(true);
                    } else {
                        player.displayClientMessage(Component.translatable("viktoria.intimate.locked"), true);
                        return;
                    }
                }
                case "intimate_pose" -> {
                    if (activateIntimate(player, companion, state, "intimate")) {
                        return;
                    }
                }
                case "intimate_close" -> {
                    if (activateIntimate(player, companion, state, "intimate_close")) {
                        return;
                    }
                }
                case "intimate_embrace" -> {
                    if (activateIntimate(player, companion, state, "intimate_embrace")) {
                        return;
                    }
                }
                default -> {
                }
            }
        }

        if (choice.nextNode() != null && !"close".equals(action)) {
            open(player, companion, choice.nextNode());
        }
    }

    private static boolean activateIntimate(ServerPlayer player, CompanionEntity companion, CompanionState state, String anim) {
        if (state.intimateConsent() && companion.character().intimateHooks().canOffer(state)) {
            companion.setAnimState(anim);
            player.displayClientMessage(Component.translatable("viktoria.intimate.active"), true);
            return true;
        }
        player.displayClientMessage(Component.translatable("viktoria.intimate.locked"), true);
        return true;
    }
}
