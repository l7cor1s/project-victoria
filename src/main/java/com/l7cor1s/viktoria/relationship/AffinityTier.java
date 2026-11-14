package com.l7cor1s.viktoria.relationship;

import net.minecraft.network.chat.Component;

public enum AffinityTier {
    STRANGER(0, "viktoria.affinity.stranger"),
    ACQUAINTANCE(15, "viktoria.affinity.acquaintance"),
    FRIEND(35, "viktoria.affinity.friend"),
    CLOSE(60, "viktoria.affinity.close"),
    PARTNER(85, "viktoria.affinity.partner");

    private final int minAffinity;
    private final String langKey;

    AffinityTier(int minAffinity, String langKey) {
        this.minAffinity = minAffinity;
        this.langKey = langKey;
    }

    public int minAffinity() {
        return minAffinity;
    }

    public Component label() {
        return Component.translatable(langKey);
    }

    public static AffinityTier from(int affinity) {
        AffinityTier best = STRANGER;
        for (AffinityTier tier : values()) {
            if (affinity >= tier.minAffinity) {
                best = tier;
            }
        }
        return best;
    }
}
