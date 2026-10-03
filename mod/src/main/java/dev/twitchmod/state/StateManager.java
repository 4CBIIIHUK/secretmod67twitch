package dev.twitchmod.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.twitchmod.TwitchMod;
import dev.twitchmod.TwitchModConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Run state storage. Written to {@code <world>/twitchmod/run.json}.
 *
 * <p>Deliberately not a PersistentState: this format survives a mod update,
 * is human readable for the editor log and cannot desync with NBT mappings.</p>
 */
public final class StateManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static RunState state;

    private StateManager() {
    }

    public static RunState get(MinecraftServer server) {
        if (state == null) {
            state = load(server);
            if (state.startedAtEpochMs == 0L) state.startedAtEpochMs = System.currentTimeMillis();
        }
        return state;
    }

    public static void save(MinecraftServer server) {
        if (state == null) return;
        try {
            Path p = file(server);
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(state));
        } catch (IOException e) {
            TwitchMod.LOGGER.warn("[twitchmod] run state save failed: {}", e.toString());
        }
    }

    public static RunState reset(MinecraftServer server) {
        state = new RunState();
        state.startedAtEpochMs = System.currentTimeMillis();
        save(server);
        return state;
    }

    /** Removes temporary event leftovers from the saved blob (spec §11). */
    public static List<String> sanitizeForSave() {
        List<String> notes = new ArrayList<>();
        if (state == null) return notes;
        if (!state.tempBlocks.isEmpty()) {
            notes.add("temp_blocks_dropped=" + state.tempBlocks.size());
            state.tempBlocks.clear();
        }
        if (state.isEventRunning()) {
            notes.add("interrupted_event=" + state.currentEventId);
            state.currentEventId = "";
            state.currentEventState = 2;
        }
        state.combatPermissions.removeIf(p -> p.expireTick <= state.activeTicks);
        return notes;
    }

    private static RunState load(MinecraftServer server) {
        Path p = file(server);
        if (!Files.exists(p)) return new RunState();
        try {
            RunState s = GSON.fromJson(Files.readString(p), RunState.class);
            if (s != null) {
                TwitchMod.LOGGER.info("[twitchmod] run restored: phase={} strikes={}/{}", s.phase, s.strikesCurrent, s.strikesTotal);
                return s;
            }
        } catch (Exception e) {
            TwitchMod.LOGGER.warn("[twitchmod] run state unreadable, fresh run started: {}", e.toString());
        }
        return new RunState();
    }

    private static Path file(MinecraftServer server) {
        Path root = server.getSavePath(WorldSavePath.ROOT);
        TwitchModConfig.get(); // touch config so defaults exist before first write
        return root.resolve("twitchmod").resolve("run.json").toAbsolutePath();
    }
}
