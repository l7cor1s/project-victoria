package com.l7cor1s.viktoria.spawn;

import com.l7cor1s.viktoria.ViktoriaMod;
import com.l7cor1s.viktoria.character.ViktoriaCharacter;
import com.l7cor1s.viktoria.entity.CompanionEntity;
import com.l7cor1s.viktoria.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Soft village presence: rare chance to spawn Viktoria near a player who is in a village
 * if she is not already loaded nearby. Custom house structure can replace this later.
 */
public final class VillageSpawnHandler {
    private static final int CHECK_INTERVAL = 200;
    private static final double CHANCE = 0.015;

    private VillageSpawnHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!level.isVillage(player.blockPosition())) {
            return;
        }
        boolean alreadyNear = !level.getEntitiesOfClass(CompanionEntity.class, player.getBoundingBox().inflate(96),
                e -> ViktoriaCharacter.ID.equals(e.getCharacterId())).isEmpty();
        if (alreadyNear) {
            return;
        }
        if (McaIntegration.trySpawnInTown(player)) {
            return;
        }
        if (player.getRandom().nextDouble() > CHANCE) {
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
