package com.l7cor1s.viktoria.spawn;

import com.l7cor1s.viktoria.ViktoriaMod;
import com.l7cor1s.viktoria.character.ViktoriaCharacter;
import com.l7cor1s.viktoria.config.ViktoriaConfig;
import com.l7cor1s.viktoria.entity.CompanionEntity;
import com.l7cor1s.viktoria.entity.ModEntities;
import com.l7cor1s.viktoria.quest.QuestProgress;
import com.l7cor1s.viktoria.relationship.CompanionState;
import com.l7cor1s.viktoria.relationship.ModAttachments;
import com.l7cor1s.viktoria.relationship.MoodType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Soft village / MCA town presence and quest night-walk progress.
 */
public final class VillageSpawnHandler {
    private static final int CHECK_INTERVAL = 200;
    private static final double VILLAGE_CHANCE = 0.02;
    private static final double MCA_TOWN_CHANCE = 0.08;

    private VillageSpawnHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get()).getOrCreate(ViktoriaCharacter.ID);
        CompanionEntity nearby = player.level().getEntitiesOfClass(CompanionEntity.class, player.getBoundingBox().inflate(24),
                        e -> ViktoriaCharacter.ID.equals(e.getCharacterId()))
                .stream().findFirst().orElse(null);
        if (nearby != null && state.following() && !player.level().isDay()) {
            QuestProgress.onFollowNightTick(player, nearby, state);
        }

        // Loneliness / neglect polish
        if (player.tickCount % 200 == 0 && state.lastInteractGameTime() > 0) {
            long idle = player.level().getGameTime() - state.lastInteractGameTime();
            if (idle > 48000L && state.mood() != MoodType.IRRITATION) {
                state.setMood(MoodType.LONELY);
                if (idle > 72000L) {
                    state.addAffinity(-1);
                }
            }
        }

        if (player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }

        double spawnScale = ViktoriaConfig.AUTO_SPAWN_CHANCE.get();
        if (McaIntegration.isPresent() && player.getRandom().nextDouble() < MCA_TOWN_CHANCE * spawnScale) {
            if (McaIntegration.trySpawnInTown(player)) {
                return;
            }
        }

        ServerLevel level = player.serverLevel();
        if (!level.isVillage(player.blockPosition()) && !McaIntegration.hasNearbyMcaTownActivity(player)) {
            return;
        }
        boolean alreadyNear = !level.getEntitiesOfClass(CompanionEntity.class, player.getBoundingBox().inflate(96),
                e -> ViktoriaCharacter.ID.equals(e.getCharacterId())).isEmpty();
        if (alreadyNear) {
            return;
        }
        if (player.getRandom().nextDouble() > VILLAGE_CHANCE * spawnScale) {
            return;
        }
        BlockPos origin = player.blockPosition().offset(
                player.getRandom().nextInt(17) - 8,
                0,
                player.getRandom().nextInt(17) - 8);
        BlockPos spawnAt = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin);
        CompanionEntity entity = ModEntities.COMPANION.get().create(level);
        if (entity == null) {
            return;
        }
        entity.setCharacterId(ViktoriaCharacter.ID);
        entity.moveTo(spawnAt.getX() + 0.5, spawnAt.getY(), spawnAt.getZ() + 0.5, player.getYRot(), 0);
        level.addFreshEntity(entity);
        ViktoriaMod.LOGGER.info("Viktoria appeared near village at {}", spawnAt);
    }
}
