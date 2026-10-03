package dev.twitchmod.director;

/** Shared counters the action guard reports into, read by running events. */
public final class EventMetrics {
    private static int blocksBrokenThisTick;
    private static int combatHitsThisTick;
    private static int itemsEatenThisTick;

    private EventMetrics() {
    }

    public static void onBreak() {
        blocksBrokenThisTick++;
    }

    public static void onCombatHit() {
        combatHitsThisTick++;
    }

    public static void onEat() {
        itemsEatenThisTick++;
    }

    public static int drainBreaks() {
        int v = blocksBrokenThisTick;
        blocksBrokenThisTick = 0;
        return v;
    }

    public static int drainHits() {
        int v = combatHitsThisTick;
        combatHitsThisTick = 0;
        return v;
    }

    public static int drainEats() {
        int v = itemsEatenThisTick;
        itemsEatenThisTick = 0;
        return v;
    }
}
