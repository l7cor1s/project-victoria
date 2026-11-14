package com.l7cor1s.viktoria.spawn;

import com.l7cor1s.viktoria.ViktoriaMod;
import com.l7cor1s.viktoria.character.ViktoriaCharacter;
import com.l7cor1s.viktoria.entity.CompanionEntity;
import com.l7cor1s.viktoria.entity.ModEntities;
import com.l7cor1s.viktoria.relationship.CompanionState;
import com.l7cor1s.viktoria.relationship.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Soft dependency on Minecraft Comes Alive (modid {@code mca}).
 * Spawns Viktoria inside MCA towns, preferring residential building centers when available.
 */
public final class McaIntegration {
    public static final String MCA_MODID = "mca";
    private static final String VILLAGE_MANAGER = "net.conczin.mca.server.world.data.VillageManager";
    private static final String VILLAGER_MCA = "net.conczin.mca.entity.VillagerEntityMCA";

    private static boolean present;
    private static Class<?> mcaVillagerClass;
    private static Method villageManagerGet;
    private static Method findNearestVillage;
    private static Method villageGetCenter;
    private static Method villageIsWithinBorder;
    private static Method villageGetBuildingsOfType;
    private static Method buildingGetCenter;

    private McaIntegration() {
    }

    public static void bootstrap() {
        present = ModList.get().isLoaded(MCA_MODID);
        if (!present) {
            ViktoriaMod.LOGGER.info("MCA not present — using house/village/command spawn.");
            return;
        }
        try {
            mcaVillagerClass = Class.forName(VILLAGER_MCA);
            Class<?> villageManagerClass = Class.forName(VILLAGE_MANAGER);
            villageManagerGet = villageManagerClass.getMethod("get", ServerLevel.class);
            findNearestVillage = villageManagerClass.getMethod("findNearestVillage", Entity.class);
            Class<?> villageClass = Class.forName("net.conczin.mca.server.world.data.Village");
            villageGetCenter = villageClass.getMethod("getCenter");
            villageIsWithinBorder = villageClass.getMethod("isWithinBorder", Entity.class);
            villageGetBuildingsOfType = villageClass.getMethod("getBuildingsOfType", String.class);
            Class<?> buildingClass = Class.forName("net.conczin.mca.server.world.data.Building");
            // Prefer getCenter if present; else synthesize from box later.
            try {
                buildingGetCenter = buildingClass.getMethod("getCenter");
            } catch (NoSuchMethodException ignored) {
                try {
                    buildingGetCenter = buildingClass.getMethod("getPos");
                } catch (NoSuchMethodException ignored2) {
                    buildingGetCenter = null;
                }
            }
            ViktoriaMod.LOGGER.info("MCA detected — town + building spawn hooks ready.");
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
                boolean nearMca = !level.getEntities(player, player.getBoundingBox().inflate(48), McaIntegration::isMcaVillager).isEmpty();
                if (!nearMca) {
                    return false;
                }
                return spawnNear(player, player.blockPosition(), state, "mca-villager-cluster");
            }
            Object village = villageOpt.get();
            Boolean inside = (Boolean) villageIsWithinBorder.invoke(village, player);
            if (inside == null || !inside) {
                return false;
            }

            Optional<BlockPos> residential = resolveResidentialSpawn(village);
            BlockPos spawnAt = residential.orElse(toBlockPos(villageGetCenter.invoke(village)));
            return spawnNear(player, spawnAt, state, "mca-town-building");
        } catch (ReflectiveOperationException ex) {
            ViktoriaMod.LOGGER.debug("MCA town spawn reflection failed: {}", ex.toString());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static Optional<BlockPos> resolveResidentialSpawn(Object village) throws ReflectiveOperationException {
        if (villageGetBuildingsOfType == null) {
            return Optional.empty();
        }
        for (String type : new String[]{"house", "home", "inn", "residential"}) {
            Object streamObj = villageGetBuildingsOfType.invoke(village, type);
            if (!(streamObj instanceof Stream<?> stream)) {
                continue;
            }
            Optional<?> first = stream.findFirst();
            if (first.isEmpty()) {
                continue;
            }
            Object building = first.get();
            if (buildingGetCenter != null) {
                return Optional.of(toBlockPos(buildingGetCenter.invoke(building)));
            }
            // Fallback: buildings map values
        }
        // Last resort: any building center from getBuildings map
        try {
            Method getBuildings = village.getClass().getMethod("getBuildings");
            Object mapObj = getBuildings.invoke(village);
            if (mapObj instanceof Map<?, ?> map && !map.isEmpty()) {
                Object building = map.values().iterator().next();
                if (buildingGetCenter != null) {
                    return Optional.of(toBlockPos(buildingGetCenter.invoke(building)));
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return Optional.empty();
    }

    private static BlockPos toBlockPos(Object centerObj) {
        if (centerObj instanceof BlockPos pos) {
            return pos;
        }
        if (centerObj instanceof Vec3i vec) {
            return new BlockPos(vec);
        }
        return BlockPos.ZERO;
    }

    private static boolean spawnNear(ServerPlayer player, BlockPos near, CompanionState state, String reason) {
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
        ViktoriaMod.LOGGER.info("Viktoria appeared via {} near {}", reason, spawnAt);
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
