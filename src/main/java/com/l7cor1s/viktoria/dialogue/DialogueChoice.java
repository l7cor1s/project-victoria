package com.l7cor1s.viktoria.dialogue;

public record DialogueChoice(
        String id,
        String langKey,
        String nextNode,
        int affinityDelta,
        String mood,
        String action
) {
    public DialogueChoice(String id, String langKey, String nextNode) {
        this(id, langKey, nextNode, 0, null, null);
    }
}
