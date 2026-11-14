package com.l7cor1s.viktoria.quest;

public record QuestDefinition(
        String id,
        String langKey,
        int minAffinity,
        int rewardAffinity
) {
}
