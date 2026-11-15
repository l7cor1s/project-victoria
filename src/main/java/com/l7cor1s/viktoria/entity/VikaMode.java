package com.l7cor1s.viktoria.entity;

import java.util.Locale;

/**
 * What Laura is currently told to do.
 *
 * @author vyrriox
 */
public enum VikaMode {
    /** Follows her partner everywhere. */
    FOLLOW,
    /** Stays exactly where she is, sitting. */
    STAY,
    /** Goes home and lives there. */
    HOME,
    /** Walks around freely near where she was left. */
    WANDER,
    /** Works in her work area (see {@link VikaJob}). */
    WORK;

    public static VikaMode byId(int id) {
        VikaMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : FOLLOW;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
