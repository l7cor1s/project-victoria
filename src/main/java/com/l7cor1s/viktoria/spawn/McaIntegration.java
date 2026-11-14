package com.l7cor1s.viktoria.spawn;

import com.l7cor1s.viktoria.ViktoriaMod;
import com.l7cor1s.viktoria.character.ViktoriaCharacter;
import com.l7cor1s.viktoria.entity.CompanionEntity;
import com.l7cor1s.viktoria.entity.ModEntities;
import com.l7cor1s.viktoria.relationship.CompanionState;
import com.l7cor1s.viktoria.relationship.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Soft dependency on Minecraft Comes Alive (modid {@code mca}).
 * When MCA is present, Viktoria can appear in MCA town borders via reflection on VillageManager.
 * Without MCA, callers fall through to village/house/command spawn.
 */
public final class McaIntegration {
    public static final String MCA_MODID = "mca";
    private static final String VILLAGE_MANAGER = "net.conczin.mca.server.world.data.VillageManager";
    private static final String VILLAGER_MCA = "net.conczin.mca.entity.VillagerEntityMCA";

    private static boolean present;
    private static Class<?> villageManagerClass;
    private static Class<?> mcaVillagerClass;
    private static Method villageManagerGet;
    private static Method findNearestVillage;
    private static Method villageGetCenter;
    private static Method villageIsWithinBorder;

    private McaIntegration() {
    }

    public static void bootstrap() {
        present = ModList.get().isLoaded(MCA_MODID);
        if (!present) {
            ViktoriaMod.LOGGER.info("MCA not present — using house/village/command spawn.");
            return;
        }
        try {
            villageManagerClass = Class.forName(VILLAGE_MANAGER);
            mcaVillagerClass = Class.forName(VILLAGER_MCA);
            villageManagerGet = villageManagerClass.getMethod("get", ServerLevel.class);
            findNearestVillage = villageManagerClass.getMethod("findNearestVillage", Entity.class);
            Class<?> villageClass = Class.forName("net.conczin.mca.server.world.data.Village");
            villageGetCenter = villageClass.getMethod("getCenter");
            villageIsWithinBorder = villageClass.getMethod("isWithinBorder", Entity.class);
            ViktoriaMod.LOGGER.info("MCA detected — town spawn reflection hooks ready.");
        } catch (ReflectiveOperationException ex) {
            ViktoriaMod.LOGGER.warn("MCA present but API hooks failed; falling back to non-MCA spawn. {}", ex.toString());
            present = false;
        }
    }

    public static boolean isPresent() {
        return present;
    }

    public static boolean isMcaVillager(Entity entity) {
        return present && mcaVillagerClass != null && mcaVillagerClass.isInstance(entity);
    }

    /**
     * @return true if Viktoria was spawned in an MCA town
     */
    public static boolean trySpawnInTown(ServerPlayer player) {
        if (!present || villageManagerGet == null) {
            return false;
        }
        CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get()).getOrCreate(ViktoriaCharacter.ID);
        if (state.metInTown()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        if (!level.getEntitiesOfClass(CompanionEntity.class, player.getBoundingBox().inflate(96),
                e -> ViktoriaCharacter.ID.equals(e.getCharacterId())).isEmpty()) {
            return false;
        }

        try {
            Object manager = villageManagerGet.invoke(null, level);
            @SuppressWarnings("unchecked")
            Optional<Object> villageOpt = (Optional<Object>) findNearestVillage.invoke(manager, player);
            if (villageOpt.isEmpty()) {
                // Fallback: near any MCA villager counts as town presence.
                boolean nearMca = !level.getEntities(player, player.getBoundingBox().inflate(48), McaIntegration::isMcaVillager).isEmpty();
                if (!nearMca) {
                    return false;
                }
                return spawnNear(player, player.blockPosition().offset(player.getRandom().nextInt(7) - 3, 0, player.getRandom().nextInt(7) - 3), state);
            }
            Object village = villageOpt.get();
            Boolean inside = (Boolean) villageIsWithinBorder.invoke(village, player);
            if (inside == null || !inside) {
                return false;
            }
            Object centerObj = villageGetCenter.invoke(village);
            BlockPos center;
            if (centerObj instanceof BlockPos pos) {
                center = pos;
            } else if (centerObj instanceof net.minecraft.core.Vec3i vec) {
                center = new BlockPos(vec);
            } else {
                center = player.blockPosition();
            }
            return spawnNear(player, center, state);
        } catch (ReflectiveOperationException ex) {
            ViktoriaMod.LOGGER.debug("MCA town spawn reflection failed: {}", ex.toString());
            return false;
        }
    }

    private static boolean spawnNear(ServerPlayer player, BlockPos near, CompanionState state) {
        ServerLevel level = player.serverLevel();
        BlockPos spawnAt = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near);
        CompanionEntity entity = ModEntities.COMPANION.get().create(level);
        if (entity == null) {
            return false;
        }
        entity.setCharacterId(ViktoriaCharacter.ID);
        entity.moveTo(spawnAt.getX() + 0.5, spawnAt.getY(), spawnAt.getZ() + 0.5, player.getYRot(), 0);
        level.addFreshEntity(entity);
        state.setMetInTown(true);
        ViktoriaMod.LOGGER.info("Viktoria appeared in MCA town near {}", spawnAt);
        return true;
    }

    public static boolean hasNearbyMcaTownActivity(ServerPlayer player) {
        if (!present) {
            return false;
        }
        AABB box = player.getBoundingBox().inflate(64);
        return !player.level().getEntities(player, box, McaIntegration::isMcaVillager).isEmpty();
    }
}
