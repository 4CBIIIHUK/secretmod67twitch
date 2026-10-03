package dev.twitchmod.client;

import java.util.ArrayList;
import java.util.List;

/** Client mirror of the last HUD packet. The client never decides anything. */
public final class ClientState {
    public static int phase;
    public static int outcome;
    public static int strikesCurrent;
    public static int strikesTotal;
    public static int combo;
    public static int attention = 100;
    public static int secondsToNextEvent;
    public static int graceSecondsLeft;
    public static int endRushSecondsLeft;
    public static int worldDamage;
    public static int worldDamageLimit = 160;
    public static int permitSecondsLeft;
    public static String crosshairKey = "tm.crosshair.ok";
    public static String permitLabel = "";
    public static String taskKey = "";
    public static boolean perfectEligible = true;
    public static boolean eventRunning;

    public static Banner banner;
    public static Choice choice;
    public static OutcomeScreenData outcomeScreen;
    public static boolean bRollMode;

    private ClientState() {
    }

    public static class Banner {
        public int kind;
        public String key;
        public int[] args;
        public long expireAt;
    }

    public static class Choice {
        public String id;
        public String titleKey;
        public boolean simulated;
        public List<String> optionKeys = new ArrayList<>();
        public List<Integer> percents = new ArrayList<>();
        public long expireAt;
    }

    public static class OutcomeScreenData {
        public int outcome;
        public String titleKey;
        public String detailKey;
        public int strikesTotal;
        public boolean perfect;
        public boolean allowObserve;
        public long expireAt;
    }

    public static boolean hasPermit() {
        return permitSecondsLeft > 0 && !permitLabel.isEmpty();
    }
}
