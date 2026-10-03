package dev.twitchmod.state;

/** Phases of a run. Every timer in the mod is expressed in active ticks. */
public enum Phase {
    INTRO,
    GRACE,
    ACTIVE,
    END_RUSH,
    OUTCOME;

    public boolean afterGrace() {
        return this == ACTIVE || this == END_RUSH || this == OUTCOME;
    }

    public boolean rulesArmed() {
        return afterGrace();
    }

    public boolean randomEventsAllowed() {
        return this == GRACE || this == ACTIVE;
    }

    public boolean isEndgame() {
        return this == END_RUSH || this == OUTCOME;
    }
}
