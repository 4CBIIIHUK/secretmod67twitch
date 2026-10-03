package dev.twitchmod.cutscene;

import dev.twitchmod.log.EditorLog;
import dev.twitchmod.net.ModNet;
import dev.twitchmod.state.RunState;
import dev.twitchmod.state.StateManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Server side of the cutscene layer. Owns the "is a scene running" flag so the
 * director never starts a chase during a scene, and never lets a fake ban run
 * with two real strikes, mid-fight or inside the End.
 */
public final class CutsceneDirector {
    private static String activeId = "";
    private static long activeUntilTick;

    private CutsceneDirector() {
    }

    public static boolean isActive() {
        return !activeId.isEmpty();
    }

    public static String activeId() {
        return activeId;
    }

    public static void tick(ServerPlayerEntity player, ServerWorld world) {
        if (activeId.isEmpty()) return;
        RunState s = StateManager.get(world.getServer());
        if (s.activeTicks >= activeUntilTick) stop(player, world);
    }

    public static boolean play(ServerPlayerEntity player, String id) {
        CutsceneDefinition def = CutsceneLibrary.byId(id);
        if (def == null) {
            EditorLog.marker("CUTSCENE", id, "MISSING", false, 0, 0, "unknown id");
            return false;
        }
        RunState s = StateManager.get(player.getServer());
        if (isTwistBlocked(def, s)) {
            EditorLog.marker("CUTSCENE", id, "SKIPPED", true, s.strikesCurrent, s.activeSeconds(), "guard");
            return false;
        }
        activeId = id;
        activeUntilTick = s.activeTicks + def.durationSec * 20L;
        if (id.startsWith("twist_fake_ban")) {
            s.fakesSeen++;
            s.flag("fake_ban_" + s.fakesSeen);
        }
        ModNet.sendCutscene(player, id);
        EditorLog.cutscene(id, true, s.strikesCurrent, s.activeSeconds());
        return true;
    }

    /** Fake bans never run with two real strikes, in combat, or in the End. */
    private static boolean isTwistBlocked(CutsceneDefinition def, RunState s) {
        if (!def.id.startsWith("twist_fake")) return false;
        if (s.strikesCurrent >= 2) return true;
        if (s.enteredEnd) return true;
        if (def.id.startsWith("twist_fake_ban") && s.fakesSeen >= 2) return true;
        long since = s.activeTicks - (s.flags.getOrDefault("last_fake_ban", -10000L));
        if (def.id.startsWith("twist_fake_ban") && since < 20L * 60L * 5L) return true;
        return false;
    }

    public static void stop(ServerPlayerEntity player, ServerWorld world) {
        if (activeId.isEmpty()) return;
        RunState s = StateManager.get(world.getServer());
        s.flags.put("last_fake_ban", s.activeTicks);
        EditorLog.cutscene(activeId, false, s.strikesCurrent, s.activeSeconds());
        activeId = "";
        activeUntilTick = 0;
        ModNet.sendCutsceneStop(player);
    }

    public static void forceStop() {
        activeId = "";
        activeUntilTick = 0;
    }
}
