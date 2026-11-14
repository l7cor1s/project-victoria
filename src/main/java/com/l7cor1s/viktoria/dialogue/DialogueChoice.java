package com.l7cor1s.viktoria.dialogue;

import java.util.List;

public record DialogueChoice(
        String id,
        String langKey,
        String nextNode,
        int affinityDelta,
        String mood,
        String action,
        int minAffinity,
        List<String> requiredMoods
) {
    public DialogueChoice(String id, String langKey, String nextNode) {
        this(id, langKey, nextNode, 0, null, null, 0, List.of());
    }

    public DialogueChoice(String id, String langKey, String nextNode, int affinityDelta, String mood, String action) {
        this(id, langKey, nextNode, affinityDelta, mood, action, 0, List.of());
    }

    public boolean visibleFor(int affinity, String currentMood) {
        if (affinity < minAffinity) {
            return false;
        }
        if (requiredMoods == null || requiredMoods.isEmpty()) {
            return true;
        }
        return requiredMoods.stream().anyMatch(m -> m.equalsIgnoreCase(currentMood));
    }
}
