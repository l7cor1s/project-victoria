package com.l7cor1s.viktoria.dialogue;

import com.l7cor1s.viktoria.relationship.MoodType;

import java.util.List;
import java.util.Map;

public record DialogueNode(
        String id,
        String textKey,
        Map<String, String> moodText,
        List<DialogueChoice> choices,
        int minAffinity,
        String requiredMood
) {
    public DialogueNode(String id, String textKey, List<DialogueChoice> choices) {
        this(id, textKey, Map.of(), choices, 0, null);
    }

    public DialogueNode(String id, String textKey, List<DialogueChoice> choices, int minAffinity, String requiredMood) {
        this(id, textKey, Map.of(), choices, minAffinity, requiredMood);
    }

    public String resolveTextKey(MoodType mood) {
        if (moodText != null && mood != null) {
            String keyed = moodText.get(mood.getSerializedName());
            if (keyed != null && !keyed.isBlank()) {
                return keyed;
            }
        }
        return textKey;
    }

    public List<DialogueChoice> visibleChoices(int affinity, MoodType mood) {
        String moodName = mood == null ? MoodType.NEUTRAL.getSerializedName() : mood.getSerializedName();
        return choices.stream().filter(c -> c.visibleFor(affinity, moodName)).toList();
    }
}
