package dev.twitchmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plain JSON config — no external config library required.
 * Covers difficulty, accessibility (subtitles / reduced motion / flashes),
 * audio separation and performance fallbacks.
 */
public class TwitchModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static TwitchModConfig instance;

    // ---- pacing -------------------------------------------------------
    public int graceSeconds = 300;
    public int finalRushSeconds = 300;
    public int eventGapMinSeconds = 30;
    public int eventGapMaxSeconds = 90;
    public int maxDangerousEventsAtOnce = 1;
    public int maxChaosInARow = 2;
    public int sameEventRepeatBlockSeconds = 480;
    public double difficultyMultiplier = 1.0D;

    // ---- rules --------------------------------------------------------
    public int strikesToBan = 3;
    public int worldDamageLimit = 160;
    public int worldDamageWindowSeconds = 600;
    public int worldDamageWarnAt = 120;
    public int armorRestoreSeconds = 30;
    public int graceCountdownSeconds = 15;
    public int graceLeatherGiftAtSecondsLeft = 30;

    // ---- attention / combo -------------------------------------------
    public int attentionIdleSeconds = 10;
    public int comboBonusEvery = 5;
    public int attentionFloorEvent = 15;

    // ---- presentation / accessibility --------------------------------
    public boolean subtitles = true;
    public boolean reducedMotion = false;
    public boolean reducedFlashes = false;
    public boolean hudIconsAlwaysShown = true;
    public double volumeModeratorVoice = 0.9D;
    public double volumeEffects = 0.8D;
    public double volumeSharpSounds = 0.6D;
    public boolean muteGameDuringSilence = true;
    public boolean silenceMutesVoiceChat = false;

    // ---- tools --------------------------------------------------------
    public boolean editorLogEnabled = true;
    public boolean editorLogJson = false;
    public boolean allowBRollMode = true;

    public static TwitchModConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("twitchmod").resolve("config.json");
    }

    public static void load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                instance = GSON.fromJson(Files.readString(p), TwitchModConfig.class);
            }
        } catch (Exception e) {
            TwitchMod.LOGGER.warn("[twitchmod] config unreadable, defaults used: {}", e.toString());
        }
        if (instance == null) instance = new TwitchModConfig();
        save();
    }

    public static void save() {
        if (instance == null) return;
        try {
            Path p = path();
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(instance));
        } catch (IOException e) {
            TwitchMod.LOGGER.warn("[twitchmod] config save failed: {}", e.toString());
        }
    }

    public static void reload() {
        load();
    }
}
