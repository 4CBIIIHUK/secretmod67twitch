package dev.twitchmod.state;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * True state of a run. Persisted as JSON inside the world folder so a reload
 * (or a crash mid-cutsene) restores exactly where the stream stopped.
 *
 * <p>Two independent counters exist on purpose:</p>
 * <ul>
 *   <li>{@link #strikesCurrent} — 0/3, drives the ban.</li>
 *   <li>{@link #strikesTotal} — drives the perfect (crown) ending; amnesty never touches it.</li>
 * </ul>
 */
public class RunState {
    public Phase phase = Phase.INTRO;
    public RunOutcome outcome = RunOutcome.NONE;

    public long activeTicks;
    public long startedAtEpochMs;
    public String lastEventId = "";

    // ---- strikes ------------------------------------------------------
    public int strikesCurrent;
    public int strikesTotal;
    public List<StrikeEntry> strikes = new ArrayList<>();
    public int amnestiesUsed;
    public int fakesSeen;
    public boolean perfectEligible = true;

    // ---- show systems -------------------------------------------------
    public int attention = 100;
    public int combo;
    public int bestCombo;
    public int eventsCompleted;
    public int eventsFailed;
    public long idleTicks;
    public long lastMeaningfulActionTick;

    // ---- grace / armor ------------------------------------------------
    public boolean leatherGifted;
    public boolean graceCountdownRunning;
    public boolean armorSetSatisfiedOnce;
    public int armorBrokenAtTick = -1;
    public boolean armorStrikeIssuedForThisGap;

    // ---- world damage -------------------------------------------------
    public List<Long> blockBreaks = new ArrayList<>();
    public long blocksBrokenTotal;
    public boolean warnedAtWorldDamage;
    public Map<String, Long> placedBlocks = new HashMap<>();
    public Set<String> sealedChests = new HashSet<>();
    public Set<String> tempBlocks = new HashSet<>();

    // ---- event director ----------------------------------------------
    public long nextEventTick = -1;
    public int chaosStreak;
    public int activeDangerousEvents;
    public String currentEventId = "";
    public long currentEventStartTick;
    public long currentEventEndTick;
    public int currentEventState; // 0 = running, 1 = success, 2 = fail
    public Map<String, Long> lastRunEventTick = new HashMap<>();
    public List<String> recentEventIds = new ArrayList<>();
    public int chainDepth;

    // ---- combat permissions (always announced on HUD) -----------------
    public List<CombatPermission> combatPermissions = new ArrayList<>();

    // ---- endgame ------------------------------------------------------
    public boolean enteredEnd;
    public long endRushStartTick = -1;
    public boolean dragonSpawnAnnounced;

    // ---- story guarantees --------------------------------------------
    public int storyStage;
    public boolean fastTravelOffered;
    public boolean endKitChecked;
    public boolean fastTravelUsed;

    // ---- misc ---------------------------------------------------------
    public boolean bRollMode;
    public boolean probationActive;
    public long probationEndTick;
    public Deque<String> markerLog = new ArrayDeque<>();
    public Map<String, Long> flags = new HashMap<>();

    public static class StrikeEntry {
        public long tick;
        public String reason;
        public boolean real = true;
        public int countAfter;

        public StrikeEntry() {
        }

        public StrikeEntry(long tick, String reason, int countAfter, boolean real) {
            this.tick = tick;
            this.reason = reason;
            this.countAfter = countAfter;
            this.real = real;
        }
    }

    public static class CombatPermission {
        public String target;
        public String label;
        public long expireTick;
        public boolean selfDefenseOnly;

        public CombatPermission() {
        }

        public CombatPermission(String target, String label, long expireTick, boolean selfDefenseOnly) {
            this.target = target;
            this.label = label;
            this.expireTick = expireTick;
            this.selfDefenseOnly = selfDefenseOnly;
        }
    }

    public long activeSeconds() {
        return activeTicks / 20L;
    }

    public boolean isEventRunning() {
        return currentEventState == 0 && !currentEventId.isEmpty() && activeTicks < currentEventEndTick;
    }

    public String flagKey(String key) {
        return key;
    }

    public void flag(String key) {
        flags.put(key, activeTicks);
    }

    public boolean flagged(String key) {
        return flags.containsKey(key);
    }

    public void markEventRan(String id) {
        lastRunEventTick.put(id, activeTicks);
        recentEventIds.add(id);
        while (recentEventIds.size() > 12) recentEventIds.remove(0);
    }

    public long ticksSinceEventRan(String id) {
        Long t = lastRunEventTick.get(id);
        return t == null ? Long.MAX_VALUE : activeTicks - t;
    }
}
