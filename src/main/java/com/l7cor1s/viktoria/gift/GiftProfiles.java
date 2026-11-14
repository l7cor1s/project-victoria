package com.l7cor1s.viktoria.gift;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Runtime gift tables keyed by character id (JSON overlays + code fallbacks). */
public final class GiftProfiles {
    private static final Map<String, GiftProfile> PROFILES = new HashMap<>();
    private static final Map<String, GiftProfile> FALLBACKS = new HashMap<>();

    private GiftProfiles() {
    }

    public static void setFallback(String characterId, GiftProfile profile) {
        FALLBACKS.put(characterId, profile);
    }

    public static void replaceLoaded(Map<String, GiftProfile> loaded) {
        PROFILES.clear();
        PROFILES.putAll(loaded);
    }

    public static Optional<GiftProfile> get(String characterId) {
        GiftProfile loaded = PROFILES.get(characterId);
        if (loaded != null) {
            return Optional.of(loaded);
        }
        return Optional.ofNullable(FALLBACKS.get(characterId));
    }

    public static GiftProfile require(String characterId) {
        return get(characterId).orElseThrow(() -> new IllegalArgumentException("No gift profile for " + characterId));
    }
}
