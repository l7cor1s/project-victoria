package com.l7cor1s.viktoria.relationship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/** Player attachment holding all companion relationship states keyed by character id. */
public final class CompanionPlayerData {
    public static final Codec<CompanionPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, CompanionState.CODEC)
                    .optionalFieldOf("companions", Map.of())
                    .forGetter(data -> data.companions)
    ).apply(instance, CompanionPlayerData::new));

    private final Map<String, CompanionState> companions;

    public CompanionPlayerData() {
        this(new HashMap<>());
    }

    public CompanionPlayerData(Map<String, CompanionState> companions) {
        this.companions = new HashMap<>();
        companions.forEach((id, state) -> this.companions.put(id, state.copy()));
    }

    public CompanionState getOrCreate(String characterId) {
        return companions.computeIfAbsent(characterId, id -> new CompanionState());
    }

    public Map<String, CompanionState> companions() {
        return companions;
    }
}
