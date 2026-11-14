package com.l7cor1s.viktoria.quest;

public record QuestDefinition(
        String id,
        String langKey,
        int minAffinity,
        int rewardAffinity,
        QuestObjective objective,
        int targetCount,
        int rewardXp,
        String targetItemId,
        String targetBiomeKey
) {
    public QuestDefinition(String id, String langKey, int minAffinity, int rewardAffinity,
                           QuestObjective objective, int targetCount) {
        this(id, langKey, minAffinity, rewardAffinity, objective, targetCount, 0, null, null);
    }

    public static QuestDefinition kill(String id, String langKey, int minAff, int rewardAff, int count, int xp) {
        return new QuestDefinition(id, langKey, minAff, rewardAff, QuestObjective.KILL_MOBS, count, xp, null, null);
    }

    public static QuestDefinition bring(String id, String langKey, int minAff, int rewardAff, int count,
                                        int xp, String itemId) {
        return new QuestDefinition(id, langKey, minAff, rewardAff, QuestObjective.BRING_ITEMS, count, xp, itemId, null);
    }

    public static QuestDefinition explore(String id, String langKey, int minAff, int rewardAff, int xp, String biomeKey) {
        return new QuestDefinition(id, langKey, minAff, rewardAff, QuestObjective.VISIT_BIOME, 1, xp, null, biomeKey);
    }
}
