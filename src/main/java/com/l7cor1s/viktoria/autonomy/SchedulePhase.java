package com.l7cor1s.viktoria.autonomy;

/** Simple day-part schedule when the player is not actively engaging Vika. */
public enum SchedulePhase {
    MORNING_WANDER,
    DAY_LOITER,
    EVENING_SUNSET,
    NIGHT_SLEEP;

    public static SchedulePhase fromDayTime(long dayTime) {
        long t = dayTime % 24000L;
        if (t < 6000L) {
            return MORNING_WANDER;
        }
        if (t < 11000L) {
            return DAY_LOITER;
        }
        if (t < 13000L) {
            return EVENING_SUNSET;
        }
        return NIGHT_SLEEP;
    }

    public String preferredAnim() {
        return switch (this) {
            case MORNING_WANDER -> "walk";
            case DAY_LOITER -> "sit";
            case EVENING_SUNSET -> "look_at_sunset";
            case NIGHT_SLEEP -> "sleep";
        };
    }
}
