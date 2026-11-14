package com.l7cor1s.viktoria.relationship;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum MoodType implements StringRepresentable {
    NEUTRAL("neutral", "viktoria.mood.neutral"),
    JOY("joy", "viktoria.mood.joy"),
    AFFECTION("affection", "viktoria.mood.affection"),
    IRRITATION("irritation", "viktoria.mood.irritation"),
    JEALOUSY("jealousy", "viktoria.mood.jealousy"),
    LONELY("lonely", "viktoria.mood.lonely");

    private final String name;
    private final String langKey;

    MoodType(String name, String langKey) {
        this.name = name;
        this.langKey = langKey;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public Component label() {
        return Component.translatable(langKey);
    }

    public static MoodType byName(String name) {
        for (MoodType mood : values()) {
            if (mood.name.equals(name)) {
                return mood;
            }
        }
        return NEUTRAL;
    }
}
