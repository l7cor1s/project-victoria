package com.l7cor1s.viktoria.relationship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/** Gift / ambient memory nested under {@link CompanionState} (keeps DFU field count ≤16). */
public final class CompanionMemory {
    private static final int MAX_RECENT_GIFTS = 8;

    public static final Codec<CompanionMemory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("lastGiftId", "").forGetter(CompanionMemory::lastGiftId),
            Codec.LONG.optionalFieldOf("lastGiftGameTime", 0L).forGetter(CompanionMemory::lastGiftGameTime),
            Codec.STRING.listOf().optionalFieldOf("recentGifts", List.of()).forGetter(CompanionMemory::recentGifts),
            Codec.BOOL.optionalFieldOf("pendingDeathReaction", false).forGetter(CompanionMemory::pendingDeathReaction),
            Codec.BOOL.optionalFieldOf("pendingHurtReaction", false).forGetter(CompanionMemory::pendingHurtReaction),
            Codec.LONG.optionalFieldOf("lastAmbientChatGameTime", 0L).forGetter(CompanionMemory::lastAmbientChatGameTime),
            Codec.STRING.listOf().optionalFieldOf("visitedBiomes", List.of()).forGetter(CompanionMemory::visitedBiomes)
    ).apply(instance, CompanionMemory::new));

    private String lastGiftId;
    private long lastGiftGameTime;
    private final List<String> recentGifts;
    private boolean pendingDeathReaction;
    private boolean pendingHurtReaction;
    private long lastAmbientChatGameTime;
    private final List<String> visitedBiomes;

    public CompanionMemory() {
        this("", 0L, List.of(), false, false, 0L, List.of());
    }

    public CompanionMemory(String lastGiftId, long lastGiftGameTime, List<String> recentGifts,
                           boolean pendingDeathReaction, boolean pendingHurtReaction,
                           long lastAmbientChatGameTime, List<String> visitedBiomes) {
        this.lastGiftId = lastGiftId == null ? "" : lastGiftId;
        this.lastGiftGameTime = lastGiftGameTime;
        this.recentGifts = new ArrayList<>(recentGifts);
        this.pendingDeathReaction = pendingDeathReaction;
        this.pendingHurtReaction = pendingHurtReaction;
        this.lastAmbientChatGameTime = lastAmbientChatGameTime;
        this.visitedBiomes = new ArrayList<>(visitedBiomes);
    }

    public String lastGiftId() {
        return lastGiftId;
    }

    public long lastGiftGameTime() {
        return lastGiftGameTime;
    }

    public List<String> recentGifts() {
        return recentGifts;
    }

    public void rememberGift(String itemId, long gameTime) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        this.lastGiftId = itemId;
        this.lastGiftGameTime = gameTime;
        recentGifts.remove(itemId);
        recentGifts.addFirst(itemId);
        while (recentGifts.size() > MAX_RECENT_GIFTS) {
            recentGifts.removeLast();
        }
    }

    public boolean pendingDeathReaction() {
        return pendingDeathReaction;
    }

    public void setPendingDeathReaction(boolean pendingDeathReaction) {
        this.pendingDeathReaction = pendingDeathReaction;
    }

    public boolean pendingHurtReaction() {
        return pendingHurtReaction;
    }

    public void setPendingHurtReaction(boolean pendingHurtReaction) {
        this.pendingHurtReaction = pendingHurtReaction;
    }

    public long lastAmbientChatGameTime() {
        return lastAmbientChatGameTime;
    }

    public void setLastAmbientChatGameTime(long lastAmbientChatGameTime) {
        this.lastAmbientChatGameTime = lastAmbientChatGameTime;
    }

    public List<String> visitedBiomes() {
        return visitedBiomes;
    }

    public boolean markBiomeVisited(String biomeId) {
        if (biomeId == null || biomeId.isBlank() || visitedBiomes.contains(biomeId)) {
            return false;
        }
        visitedBiomes.add(biomeId);
        return true;
    }

    public CompanionMemory copy() {
        return new CompanionMemory(lastGiftId, lastGiftGameTime, recentGifts,
                pendingDeathReaction, pendingHurtReaction, lastAmbientChatGameTime, visitedBiomes);
    }
}
