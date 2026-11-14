package com.l7cor1s.viktoria.relationship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Per-character relationship blob persisted on the player. */
public final class CompanionState {
    public static final Codec<CompanionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("affinity", 0).forGetter(CompanionState::affinity),
            Codec.STRING.optionalFieldOf("mood", MoodType.NEUTRAL.getSerializedName()).forGetter(s -> s.mood().getSerializedName()),
            Codec.LONG.optionalFieldOf("lastInteractGameTime", 0L).forGetter(CompanionState::lastInteractGameTime),
            Codec.BOOL.optionalFieldOf("following", false).forGetter(CompanionState::following),
            Codec.BOOL.optionalFieldOf("livesTogether", false).forGetter(CompanionState::livesTogether),
            BlockPos.CODEC.optionalFieldOf("homePos").forGetter(s -> Optional.ofNullable(s.homePos())),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("quests", Map.of()).forGetter(CompanionState::questProgress),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("questCounters", Map.of()).forGetter(CompanionState::questCounters),
            Codec.BOOL.optionalFieldOf("intimateConsent", false).forGetter(CompanionState::intimateConsent),
            Codec.BOOL.optionalFieldOf("metInTown", false).forGetter(CompanionState::metInTown),
            CompanionMemory.CODEC.optionalFieldOf("memory", new CompanionMemory()).forGetter(CompanionState::memory)
    ).apply(instance, CompanionState::fromCodec));

    private int affinity;
    private MoodType mood;
    private long lastInteractGameTime;
    private boolean following;
    private boolean livesTogether;
    private BlockPos homePos;
    private final Map<String, Integer> questProgress;
    private final Map<String, Integer> questCounters;
    private boolean intimateConsent;
    private boolean metInTown;
    private final CompanionMemory memory;

    public CompanionState() {
        this(0, MoodType.NEUTRAL, 0L, false, false, null, new HashMap<>(), new HashMap<>(),
                false, false, new CompanionMemory());
    }

    private CompanionState(int affinity, MoodType mood, long lastInteractGameTime, boolean following,
                           boolean livesTogether, BlockPos homePos, Map<String, Integer> questProgress,
                           Map<String, Integer> questCounters, boolean intimateConsent, boolean metInTown,
                           CompanionMemory memory) {
        this.affinity = Math.max(0, Math.min(100, affinity));
        this.mood = mood;
        this.lastInteractGameTime = lastInteractGameTime;
        this.following = following;
        this.livesTogether = livesTogether;
        this.homePos = homePos;
        this.questProgress = new HashMap<>(questProgress);
        this.questCounters = new HashMap<>(questCounters);
        this.intimateConsent = intimateConsent;
        this.metInTown = metInTown;
        this.memory = memory == null ? new CompanionMemory() : memory;
    }

    private static CompanionState fromCodec(int affinity, String mood, long lastInteract, boolean following,
                                            boolean livesTogether, Optional<BlockPos> home,
                                            Map<String, Integer> quests, Map<String, Integer> counters,
                                            boolean intimateConsent, boolean metInTown, CompanionMemory memory) {
        return new CompanionState(affinity, MoodType.byName(mood), lastInteract, following, livesTogether,
                home.orElse(null), quests, counters, intimateConsent, metInTown, memory);
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

    public Map<String, Integer> questCounters() {
        return questCounters;
    }

    public int questStage(String questId) {
        return questProgress.getOrDefault(questId, 0);
    }

    public void setQuestStage(String questId, int stage) {
        questProgress.put(questId, stage);
    }

    public int questCounter(String questId) {
        return questCounters.getOrDefault(questId, 0);
    }

    public void setQuestCounter(String questId, int value) {
        questCounters.put(questId, value);
    }

    public boolean intimateConsent() {
        return intimateConsent;
    }

    public void setIntimateConsent(boolean intimateConsent) {
        this.intimateConsent = intimateConsent;
    }

    public boolean metInTown() {
        return metInTown;
    }

    public void setMetInTown(boolean metInTown) {
        this.metInTown = metInTown;
    }

    public CompanionMemory memory() {
        return memory;
    }

    public String lastGiftId() {
        return memory.lastGiftId();
    }

    public long lastGiftGameTime() {
        return memory.lastGiftGameTime();
    }

    public List<String> recentGifts() {
        return memory.recentGifts();
    }

    public void rememberGift(String itemId, long gameTime) {
        memory.rememberGift(itemId, gameTime);
    }

    public boolean pendingDeathReaction() {
        return memory.pendingDeathReaction();
    }

    public void setPendingDeathReaction(boolean value) {
        memory.setPendingDeathReaction(value);
    }

    public boolean pendingHurtReaction() {
        return memory.pendingHurtReaction();
    }

    public void setPendingHurtReaction(boolean value) {
        memory.setPendingHurtReaction(value);
    }

    public long lastAmbientChatGameTime() {
        return memory.lastAmbientChatGameTime();
    }

    public void setLastAmbientChatGameTime(long value) {
        memory.setLastAmbientChatGameTime(value);
    }

    public List<String> visitedBiomes() {
        return memory.visitedBiomes();
    }

    public boolean markBiomeVisited(String biomeId) {
        return memory.markBiomeVisited(biomeId);
    }

    public CompanionState copy() {
        return new CompanionState(affinity, mood, lastInteractGameTime, following, livesTogether, homePos,
                questProgress, questCounters, intimateConsent, metInTown, memory.copy());
    }
}
