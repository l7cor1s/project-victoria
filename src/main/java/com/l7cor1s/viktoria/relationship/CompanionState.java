package com.l7cor1s.viktoria.relationship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Per-character relationship blob persisted on the player. */
public final class CompanionState {
    public static final Codec<CompanionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("affinity", 0).forGetter(CompanionState::affinity),
            Codec.STRING.optionalFieldOf("mood", MoodType.NEUTRAL.getSerializedName()).forGetter(s -> s.mood.getSerializedName()),
            Codec.LONG.optionalFieldOf("lastInteractGameTime", 0L).forGetter(CompanionState::lastInteractGameTime),
            Codec.BOOL.optionalFieldOf("following", false).forGetter(CompanionState::following),
            Codec.BOOL.optionalFieldOf("livesTogether", false).forGetter(CompanionState::livesTogether),
            BlockPos.CODEC.optionalFieldOf("homePos").forGetter(s -> Optional.ofNullable(s.homePos)),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("quests", Map.of()).forGetter(CompanionState::questProgress),
            Codec.BOOL.optionalFieldOf("intimateConsent", false).forGetter(CompanionState::intimateConsent)
    ).apply(instance, CompanionState::fromCodec));

    private int affinity;
    private MoodType mood;
    private long lastInteractGameTime;
    private boolean following;
    private boolean livesTogether;
    private BlockPos homePos;
    private final Map<String, Integer> questProgress;
    private boolean intimateConsent;

    public CompanionState() {
        this(0, MoodType.NEUTRAL, 0L, false, false, null, new HashMap<>(), false);
    }

    private CompanionState(int affinity, MoodType mood, long lastInteractGameTime, boolean following,
                           boolean livesTogether, BlockPos homePos, Map<String, Integer> questProgress,
                           boolean intimateConsent) {
        this.affinity = Math.max(0, Math.min(100, affinity));
        this.mood = mood;
        this.lastInteractGameTime = lastInteractGameTime;
        this.following = following;
        this.livesTogether = livesTogether;
        this.homePos = homePos;
        this.questProgress = new HashMap<>(questProgress);
        this.intimateConsent = intimateConsent;
    }

    private static CompanionState fromCodec(int affinity, String mood, long lastInteract, boolean following,
                                            boolean livesTogether, Optional<BlockPos> home,
                                            Map<String, Integer> quests, boolean intimateConsent) {
        return new CompanionState(affinity, MoodType.byName(mood), lastInteract, following, livesTogether,
                home.orElse(null), quests, intimateConsent);
    }

    public int affinity() {
        return affinity;
    }

    public AffinityTier tier() {
        return AffinityTier.from(affinity);
    }

    public void addAffinity(int delta) {
        affinity = Math.max(0, Math.min(100, affinity + delta));
    }

    public MoodType mood() {
        return mood;
    }

    public void setMood(MoodType mood) {
        this.mood = mood == null ? MoodType.NEUTRAL : mood;
    }

    public long lastInteractGameTime() {
        return lastInteractGameTime;
    }

    public void touch(long gameTime) {
        this.lastInteractGameTime = gameTime;
    }

    public boolean following() {
        return following;
    }

    public void setFollowing(boolean following) {
        this.following = following;
    }

    public boolean livesTogether() {
        return livesTogether;
    }

    public void setLivesTogether(boolean livesTogether) {
        this.livesTogether = livesTogether;
    }

    public BlockPos homePos() {
        return homePos;
    }

    public void setHomePos(BlockPos homePos) {
        this.homePos = homePos;
    }

    public Map<String, Integer> questProgress() {
        return questProgress;
    }

    public int questStage(String questId) {
        return questProgress.getOrDefault(questId, 0);
    }

    public void setQuestStage(String questId, int stage) {
        questProgress.put(questId, stage);
    }

    public boolean intimateConsent() {
        return intimateConsent;
    }

    public void setIntimateConsent(boolean intimateConsent) {
        this.intimateConsent = intimateConsent;
    }

    public CompanionState copy() {
        return new CompanionState(affinity, mood, lastInteractGameTime, following, livesTogether, homePos,
                questProgress, intimateConsent);
    }
}
