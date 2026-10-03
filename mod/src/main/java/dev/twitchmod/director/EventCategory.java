package dev.twitchmod.director;

/** Eight fixed categories of the catalogue (spec §6). */
public enum EventCategory {
    GIFT(45, "gift"),
    STORY(20, "story"),
    CHAOS(50, "chaos"),
    MINIGAME(25, "minigame"),
    MODERATION(20, "moderation"),
    ATMOSPHERE(30, "atmosphere"),
    SECRET(15, "secret"),
    CHAT(20, "chat");

    public final int targetCount;
    public final String key;

    EventCategory(int targetCount, String key) {
        this.targetCount = targetCount;
        this.key = key;
    }

    public boolean isDangerous() {
        return this == CHAOS || this == MINIGAME;
    }
}
