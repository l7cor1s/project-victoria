package com.l7cor1s.viktoria.entity;

import java.util.Locale;

/**
 * Work Laura can do on her own.
 *
 * @author vyrriox
 */
public enum VikaJob {
    NONE,
    /** Fells natural trees, replants saplings, stores the wood. */
    LUMBERJACK,
    /** Harvests and replants the crops of a field, stores the harvest. */
    FARMER,
    /** Cooks raw food in furnaces, smokers and campfires, prepares meals, stores them. */
    COOK;

    public static VikaJob byId(int id) {
        VikaJob[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }

    public static VikaJob byName(String name) {
        for (VikaJob job : values()) {
            if (job.name().equalsIgnoreCase(name)) {
                return job;
            }
        }
        return null;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
