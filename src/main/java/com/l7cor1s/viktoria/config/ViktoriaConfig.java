package com.l7cor1s.viktoria.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common config — Cloth Config UI binds to these values on the client. */
public final class ViktoriaConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue AUTONOMY_ENABLED;
    public static final ModConfigSpec.BooleanValue DISTANT_CHAT_ENABLED;
    public static final ModConfigSpec.BooleanValue WEATHER_REACTIONS;
    public static final ModConfigSpec.BooleanValue BIOME_REACTIONS;
    public static final ModConfigSpec.BooleanValue INTIMATE_SCENES_ENABLED;
    public static final ModConfigSpec.DoubleValue AUTO_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue AFFINITY_FRIEND_THRESHOLD;
    public static final ModConfigSpec.IntValue AFFINITY_CLOSE_THRESHOLD;
    public static final ModConfigSpec.IntValue AFFINITY_PARTNER_THRESHOLD;
    public static final ModConfigSpec.IntValue DISTANT_CHAT_RANGE;
    public static final ModConfigSpec.IntValue DISTANT_CHAT_COOLDOWN_TICKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("behavior");
        AUTONOMY_ENABLED = b.comment("Autonomous schedule: wander / sit / sunset / sleep when idle")
                .define("autonomyEnabled", true);
        DISTANT_CHAT_ENABLED = b.comment("Occasionally approach or chat when the player is far")
                .define("distantChatEnabled", true);
        WEATHER_REACTIONS = b.comment("React to rain / thunder")
                .define("weatherReactions", true);
        BIOME_REACTIONS = b.comment("Ambient biome flavour lines")
                .define("biomeReactions", true);
        DISTANT_CHAT_RANGE = b.defineInRange("distantChatRange", 28, 12, 96);
        DISTANT_CHAT_COOLDOWN_TICKS = b.defineInRange("distantChatCooldownTicks", 6000, 200, 24000);
        b.pop();

        b.push("spawn");
        AUTO_SPAWN_CHANCE = b.comment("Chance to auto-spawn near villages / cottages (0–1)")
                .defineInRange("autoSpawnChance", 0.65d, 0.0d, 1.0d);
        b.pop();

        b.push("affinity");
        AFFINITY_FRIEND_THRESHOLD = b.defineInRange("friendThreshold", 35, 1, 99);
        AFFINITY_CLOSE_THRESHOLD = b.defineInRange("closeThreshold", 60, 1, 99);
        AFFINITY_PARTNER_THRESHOLD = b.defineInRange("partnerThreshold", 85, 1, 99);
        b.pop();

        b.push("intimate");
        INTIMATE_SCENES_ENABLED = b.comment("Allow intimate dialogue / poses when affinity is high")
                .define("intimateScenesEnabled", true);
        b.pop();

        SPEC = b.build();
    }

    private ViktoriaConfig() {
    }

    public static boolean autonomyEnabled() {
        return AUTONOMY_ENABLED.get();
    }

    public static boolean intimateEnabled() {
        return INTIMATE_SCENES_ENABLED.get();
    }
}
