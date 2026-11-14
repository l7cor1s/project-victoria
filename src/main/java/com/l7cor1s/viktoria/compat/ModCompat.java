package com.l7cor1s.viktoria.compat;

import com.l7cor1s.viktoria.ViktoriaMod;
import net.neoforged.fml.ModList;

/**
 * Soft-dependency probes. The mod runs fully without these; when present we add flavour.
 */
public final class ModCompat {
    public static final String CREATE = "create";
    public static final String IRONS_SPELLS = "irons_spellbooks";
    public static final String FARMERS_DELIGHT = "farmersdelight";
    public static final String KALEIDOSCOPE = "kaleidoscope_cookery";
    public static final String PLAYER_ANIM = "playeranimator";
    public static final String MCA = "mca";

    private static boolean create;
    private static boolean irons;
    private static boolean farmersDelight;
    private static boolean kaleidoscope;
    private static boolean playerAnim;
    private static boolean mca;

    private ModCompat() {
    }

    public static void bootstrap() {
        create = ModList.get().isLoaded(CREATE);
        irons = ModList.get().isLoaded(IRONS_SPELLS);
        farmersDelight = ModList.get().isLoaded(FARMERS_DELIGHT);
        kaleidoscope = ModList.get().isLoaded(KALEIDOSCOPE);
        playerAnim = ModList.get().isLoaded(PLAYER_ANIM);
        mca = ModList.get().isLoaded(MCA);
        ViktoriaMod.LOGGER.info(
                "Soft deps — create={} irons={} farmersdelight={} kaleidoscope={} playeranimator={} mca={}",
                create, irons, farmersDelight, kaleidoscope, playerAnim, mca);
    }

    public static boolean create() {
        return create;
    }

    public static boolean ironsSpells() {
        return irons;
    }

    public static boolean foodModPresent() {
        return farmersDelight || kaleidoscope;
    }

    public static boolean farmersDelight() {
        return farmersDelight;
    }

    public static boolean playerAnimator() {
        return playerAnim;
    }

    public static boolean mca() {
        return mca;
    }
}
