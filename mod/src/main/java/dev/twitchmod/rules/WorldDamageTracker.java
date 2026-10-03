package dev.twitchmod.rules;

import dev.twitchmod.state.RunState;
import dev.twitchmod.TwitchModConfig;

/** Rolling 10 minute window of player-broken blocks. */
public final class WorldDamageTracker {
    private WorldDamageTracker() {
    }

    public static void prune(RunState s) {
        long windowTicks = TwitchModConfig.get().worldDamageWindowSeconds * 20L;
        long cutoff = s.activeTicks - windowTicks;
        s.blockBreaks.removeIf(t -> t < cutoff);
        if (countInWindow(s) < TwitchModConfig.get().worldDamageLimit) {
            s.warnedAtWorldDamage = false;
        }
    }

    public static int countInWindow(RunState s) {
        return s.blockBreaks.size();
    }

    public static void record(RunState s) {
        s.blockBreaks.add(s.activeTicks);
        s.blocksBrokenTotal++;
    }

    public static boolean warnThresholdReached(RunState s) {
        return countInWindow(s) >= TwitchModConfig.get().worldDamageWarnAt && !s.warnedAtWorldDamage;
    }

    public static boolean limitReached(RunState s) {
        return countInWindow(s) >= TwitchModConfig.get().worldDamageLimit;
    }
}
