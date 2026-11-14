package com.l7cor1s.viktoria.spawn;

import com.l7cor1s.viktoria.ViktoriaMod;
import net.neoforged.fml.ModList;

/**
 * Soft-dependency stub for Minecraft Comes Alive town spawn.
 * When MCA is present on NeoForge 1.21.1 with a usable API, wire village/NPC injection here.
 * Until then, village structure chance + /viktoria summon remain the spawn paths.
 */
public final class McaIntegration {
    public static final String MCA_MODID = "mca";
    private static boolean present;

    private McaIntegration() {
    }

    public static void bootstrap() {
        present = ModList.get().isLoaded(MCA_MODID);
        if (present) {
            ViktoriaMod.LOGGER.info("MCA detected — town spawn hook stub active (awaiting MCA 1.21.1 API).");
        } else {
            ViktoriaMod.LOGGER.info("MCA not present — using village/command spawn.");
        }
    }

    public static boolean isPresent() {
        return present;
    }

    /**
     * Future: inject Viktoria into MCA town resident lists / buildings.
     * @return true if an MCA-side spawn was performed
     */
    public static boolean trySpawnInTown(Object townContext) {
        if (!present) {
            return false;
        }
        // Stub: MCA API surface for NeoForge 1.21.1 is not wired yet.
        ViktoriaMod.LOGGER.debug("MCA town spawn stub called; no-op until API integration.");
        return false;
    }
}
