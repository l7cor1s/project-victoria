package com.l7cor1s.viktoria.network;

/**
 * Orders a player can give Laura from the menu, the keybind or commands.
 *
 * @author vyrriox
 */
public enum VikaAction {
    FOLLOW,
    STAY,
    WANDER,
    HOME,
    SET_HOME,
    CLEAR_HOME,
    COME,
    FETCH,
    EMOTE,
    HUG,
    KISS,
    UNGAG,
    INVENTORY,
    COMBAT,
    PICKUP,
    INFO,
    ANSWER,
    EAT,
    STOP,
    RENAME,
    SLEEP,
    WAKE_UP,
    COMPLIMENT,
    /** Argument "JOB:on:radius", "JOB:off" or "ALL:off". */
    JOB,
    /** Back to her jobs. */
    WORK,
    /** Argument "TYPE:now" or "TYPE:queue", optionally followed by ":radius". */
    TASK,
    /** Argument "clear" or "remove:index" (1 based). */
    QUEUE,
    /** Argument a chest purpose, or "remove", applied to the container the player looks at. */
    CHEST;

    public static VikaAction byId(int id) {
        VikaAction[] values = values();
        return id >= 0 && id < values.length ? values[id] : null;
    }
}
