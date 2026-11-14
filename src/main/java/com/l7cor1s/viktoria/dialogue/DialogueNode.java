package com.l7cor1s.viktoria.dialogue;

import java.util.List;

public record DialogueNode(
        String id,
        String textKey,
        List<DialogueChoice> choices,
        int minAffinity,
        String requiredMood
) {
    public DialogueNode(String id, String textKey, List<DialogueChoice> choices) {
        this(id, textKey, choices, 0, null);
    }
}
