package com.l7cor1s.viktoria.intimate;

import com.l7cor1s.viktoria.relationship.CompanionState;

/**
 * Gate intimate / special interactions behind affinity tier + explicit consent dialogue.
 * Pose/animation hooks are activated on the entity when consent is granted.
 */
public final class IntimateHooks {
    private final String characterId;
    private final int minAffinity;

    private IntimateHooks(String characterId, int minAffinity) {
        this.characterId = characterId;
        this.minAffinity = minAffinity;
    }

    public static IntimateHooks forCharacter(String characterId, int minAffinity) {
        return new IntimateHooks(characterId, minAffinity);
    }

    public String characterId() {
        return characterId;
    }

    public int minAffinity() {
        return minAffinity;
    }

    public boolean canOffer(CompanionState state) {
        return state.affinity() >= minAffinity;
    }
}
