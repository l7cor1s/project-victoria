package com.l7cor1s.viktoria.autonomy;

import com.l7cor1s.viktoria.config.ViktoriaConfig;
import com.l7cor1s.viktoria.entity.CompanionEntity;
import com.l7cor1s.viktoria.relationship.CompanionState;
import com.l7cor1s.viktoria.relationship.ModAttachments;
import com.l7cor1s.viktoria.relationship.MoodType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Idle-life loop: schedule poses, weather shelter, biome flavour, distant pings.
 * Skipped while following / talking / gift / intimate.
 */
public final class CompanionAutonomy {
    private CompanionAutonomy() {
    }

    public static void tick(CompanionEntity companion) {
        if (!(companion.level() instanceof ServerLevel level)) {
            return;
        }
        if (!ViktoriaConfig.autonomyEnabled()) {
            return;
        }
        if (companion.tickCount % 20 != 0) {
            return;
        }

        String anim = companion.getAnimState();
        if (isBusy(anim) || companion.isFollowing()) {
            return;
        }

        ServerPlayer nearest = (ServerPlayer) level.getNearestPlayer(companion, 64.0);
        handleWeather(companion, level, nearest);
        handleSchedule(companion, level);
        if (nearest != null) {
            handleDistant(companion, level, nearest);
            handleBiomeFlavour(companion, level, nearest);
        }
    }

    private static boolean isBusy(String anim) {
        return "talk".equals(anim)
                || "accept_gift".equals(anim)
                || "gift".equals(anim)
                || "intimate".equals(anim)
                || "intimate_close".equals(anim)
                || "intimate_embrace".equals(anim);
    }

    private static void handleSchedule(CompanionEntity companion, ServerLevel level) {
        SchedulePhase phase = SchedulePhase.fromDayTime(level.getDayTime());
        String want = phase.preferredAnim();

        if (level.isRaining() && ViktoriaConfig.WEATHER_REACTIONS.get()) {
            // Prefer sit / shelter over sunset when raining.
            if (phase == SchedulePhase.EVENING_SUNSET || phase == SchedulePhase.MORNING_WANDER) {
                want = "sit";
            }
        }

        if (phase == SchedulePhase.MORNING_WANDER || phase == SchedulePhase.DAY_LOITER) {
            if (companion.getNavigation().isDone() && companion.getRandom().nextFloat() < 0.35f) {
                Vec3 target = DefaultRandomPos.getPos(companion, 8, 4);
                if (target != null) {
                    companion.getNavigation().moveTo(target.x, target.y, target.z, 0.75);
                }
            }
        }

        if (phase == SchedulePhase.NIGHT_SLEEP || phase == SchedulePhase.EVENING_SUNSET) {
            companion.getNavigation().stop();
        }

        if (!want.equals(companion.getAnimState()) && !companion.getNavigation().isInProgress()) {
            companion.setAnimState(want);
        } else if (companion.getNavigation().isInProgress() && phase == SchedulePhase.MORNING_WANDER) {
            companion.setAnimState("walk");
        }
    }

    private static void handleWeather(CompanionEntity companion, ServerLevel level, ServerPlayer player) {
        if (!ViktoriaConfig.WEATHER_REACTIONS.get() || !level.isRaining()) {
            return;
        }
        if (companion.tickCount % 200 != 0) {
            return;
        }
        // Seek under a solid roof if exposed.
        if (level.canSeeSky(companion.blockPosition())) {
            BlockPos shelter = findShelter(level, companion.blockPosition(), 10);
            if (shelter != null) {
                companion.getNavigation().moveTo(shelter.getX() + 0.5, shelter.getY(), shelter.getZ() + 0.5, 1.05);
                companion.setAnimState("walk");
            }
        }
        if (player != null && companion.distanceTo(player) < 16.0f && companion.getRandom().nextFloat() < 0.25f) {
            player.displayClientMessage(Component.translatable("viktoria.ambient.rain",
                    companion.character().displayName()), true);
        }
    }

    private static BlockPos findShelter(ServerLevel level, BlockPos origin, int radius) {
        for (int i = 0; i < 24; i++) {
            int x = origin.getX() + level.random.nextInt(radius * 2 + 1) - radius;
            int z = origin.getZ() + level.random.nextInt(radius * 2 + 1) - radius;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.canSeeSky(pos) && level.getBlockState(pos).isAir()) {
                return pos;
            }
            BlockPos below = pos.below();
            if (!level.canSeeSky(below.above()) && level.getBlockState(below.above()).isAir()) {
                return below.above();
            }
        }
        return null;
    }

    private static void handleDistant(CompanionEntity companion, ServerLevel level, ServerPlayer player) {
        if (!ViktoriaConfig.DISTANT_CHAT_ENABLED.get()) {
            return;
        }
        double dist = companion.distanceTo(player);
        int range = ViktoriaConfig.DISTANT_CHAT_RANGE.get();
        if (dist < range) {
            return;
        }
        CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get())
                .getOrCreate(companion.getCharacterId());
        long now = level.getGameTime();
        if (now - state.lastAmbientChatGameTime() < ViktoriaConfig.DISTANT_CHAT_COOLDOWN_TICKS.get()) {
            return;
        }
        if (companion.getRandom().nextFloat() > 0.2f) {
            return;
        }
        state.setLastAmbientChatGameTime(now);
        if (companion.getRandom().nextBoolean() && dist < range * 1.6) {
            companion.getNavigation().moveTo(player, 1.1);
            companion.setAnimState("walk");
            player.displayClientMessage(Component.translatable("viktoria.ambient.approach",
                    companion.character().displayName()), true);
        } else {
            player.displayClientMessage(Component.translatable("viktoria.ambient.miss_you",
                    companion.character().displayName()), false);
            if (state.mood() != MoodType.IRRITATION) {
                state.setMood(MoodType.LONELY);
            }
        }
    }

    private static void handleBiomeFlavour(CompanionEntity companion, ServerLevel level, ServerPlayer player) {
        if (!ViktoriaConfig.BIOME_REACTIONS.get() || companion.tickCount % 300 != 0) {
            return;
        }
        if (companion.distanceTo(player) > 12.0f || companion.getRandom().nextFloat() > 0.15f) {
            return;
        }
        Holder<Biome> biome = level.getBiome(companion.blockPosition());
        String key = biomeKey(biome);
        CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get())
                .getOrCreate(companion.getCharacterId());
        state.markBiomeVisited(key);

        String line;
        if (biome.is(BiomeTags.IS_FOREST) || key.contains("forest")) {
            line = "viktoria.ambient.biome.forest";
        } else if (key.contains("desert") || key.contains("badlands")) {
            line = "viktoria.ambient.biome.desert";
        } else if (key.contains("snow") || key.contains("frozen") || key.contains("ice") || key.contains("grove")) {
            line = "viktoria.ambient.biome.snow";
        } else if (biome.is(BiomeTags.IS_OCEAN) || key.contains("ocean") || key.contains("beach")) {
            line = "viktoria.ambient.biome.ocean";
        } else {
            return;
        }
        player.displayClientMessage(Component.translatable(line, companion.character().displayName()), true);
    }

    public static String biomeKey(Holder<Biome> biome) {
        return biome.unwrapKey()
                .map(ResourceKey::location)
                .map(ResourceLocation::toString)
                .orElse("unknown");
    }
}
