package com.l7cor1s.viktoria.character;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CharacterRegistry {
    private static final Map<String, CompanionCharacter> CHARACTERS = new LinkedHashMap<>();

    private CharacterRegistry() {
    }

    public static void register(CompanionCharacter character) {
        if (CHARACTERS.containsKey(character.id())) {
            throw new IllegalStateException("Duplicate companion character id: " + character.id());
        }
        CHARACTERS.put(character.id(), character);
    }

    public static Optional<CompanionCharacter> get(String id) {
        return Optional.ofNullable(CHARACTERS.get(id));
    }

    public static CompanionCharacter require(String id) {
        return get(id).orElseThrow(() -> new IllegalArgumentException("Unknown companion: " + id));
    }

    public static Collection<CompanionCharacter> all() {
        return Collections.unmodifiableCollection(CHARACTERS.values());
    }

    public static Set<String> ids() {
        return Collections.unmodifiableSet(CHARACTERS.keySet());
    }
}
