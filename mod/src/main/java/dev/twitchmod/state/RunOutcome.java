package dev.twitchmod.state;

/**
 * Terminal states of a run. A run never gets a fake third strike and never
 * deletes the world file — ban only blocks the challenge.
 */
public enum RunOutcome {
    NONE,
    WIN,
    PERFECT_WIN,
    BANNED,
    DIED,
    AIRTIME_OVER,
    INVALID_FOR_LEADERBOARD;

    public boolean isTerminal() {
        return this != NONE;
    }
}
