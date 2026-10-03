package dev.twitchmod.director;

/**
 * A data-described event. No free-form strings like {@code "condition": "time > 600"}
 * and no executable JSON — the loader validates every field of every record.
 */
public class EventDefinition {
    public String id;
    public EventCategory category;

    public int weight = 3;
    public int cooldownSec = 600;
    public int telegraphSec = 4;
    public int durationSec = 30;

    /** earliest active second this record may fire (story guarantees use this) */
    public int minActiveSec;
    public String dimension = "overworld";

    // ---- conditions ---------------------------------------------------
    public int minHealth;
    public int minArmorPieces;
    public boolean needsSafeEscape;
    public boolean needsHostileFreeArea;
    public boolean requiresDarkness;
    public boolean requiresLight;

    // ---- execution ----------------------------------------------------
    public boolean dangerous;
    public boolean grantsCombat;
    public String objective = "none";
    public int objectiveAmount;
    public String objectiveTarget = "";
    public String rewardTable = "common";
    public String failPenalty = "attention";
    public String cutsceneId = "";
    /** Human readable titles live in the catalogue itself so the registry can be exported. */
    public String title = "";
    public String desc = "";

    public Action[] actions = new Action[0];
    public Action[] onSuccess = new Action[0];
    public Action[] onFail = new Action[0];
    public String[] cleanup = new String[0];

    public static class Action {
        public String type;
        public int a;
        public int b;
        public String s;

        public Action() {
        }

        public Action(String type, int a, int b, String s) {
            this.type = type;
            this.a = a;
            this.b = b;
            this.s = s;
        }
    }

    public boolean isObjectiveDriven() {
        return !"none".equals(objective) && !"survive".equals(objective);
    }

    @Override
    public String toString() {
        return id + "[" + category.key + "]";
    }
}
