package dev.twitchmod.attention;

import dev.twitchmod.TwitchModConfig;
import dev.twitchmod.director.EventDirector;
import dev.twitchmod.net.ModNet;
import dev.twitchmod.state.RunState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Attention meter. Drops after 10 s of standing still, grows on meaningful
 * actions. Zero triggers a notification and a director nudge — never a strike.
 * Pauses, cutscenes and the "don't move" mini-game are excluded.
 */
public final class AttentionManager {
    private static final Map<UUID, Vec3d> LAST_POS = new HashMap<>();
    private static final Map<UUID, Boolean> IDLE_SHOWN = new HashMap<>();

    private AttentionManager() {
    }

    public static void tick(ServerPlayerEntity p, RunState s, boolean cutsceneOrMinigame) {
        if (s.phase == dev.twitchmod.state.Phase.OUTCOME) return;
        Vec3d now = p.getPos();
        Vec3d last = LAST_POS.put(p.getUuid(), now);
        boolean moved = last == null || last.squaredDistanceTo(now) > 0.004D;

        if (moved || cutsceneOrMinigame) {
            s.idleTicks = 0;
            if (cutsceneOrMinigame) return;
        } else {
            s.idleTicks++;
        }

        long idleLimit = TwitchModConfig.get().attentionIdleSeconds * 20L;
        if (s.idleTicks > idleLimit) {
            if (s.activeTicks % 20L == 0L) add(s, -2);
        } else if (s.activeTicks % 40L == 0L) {
            add(s, 1);
        }

        if (s.attention <= 0 && !IDLE_SHOWN.getOrDefault(p.getUuid(), false)) {
            IDLE_SHOWN.put(p.getUuid(), true);
            ModNet.sendBanner(p, 1, "tm.attention.low", 5, 0);
            s.attention = 25;
            EventDirector.scheduleNext(s, TwitchModConfig.get().attentionFloorEvent);
        } else if (s.attention > 30) {
            IDLE_SHOWN.put(p.getUuid(), false);
        }
    }

    public static void meaningful(RunState s) {
        s.idleTicks = 0;
        s.lastMeaningfulActionTick = s.activeTicks;
        add(s, 4);
    }

    public static void add(RunState s, int delta) {
        s.attention = Math.max(0, Math.min(100, s.attention + delta));
    }
}
