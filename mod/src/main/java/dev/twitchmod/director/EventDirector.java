package dev.twitchmod.director;

import dev.twitchmod.TwitchModConfig;
import dev.twitchmod.attention.AttentionManager;
import dev.twitchmod.attention.ComboManager;
import dev.twitchmod.cutscene.CutsceneDirector;
import dev.twitchmod.log.EditorLog;
import dev.twitchmod.net.ModNet;
import dev.twitchmod.rules.CombatPermissions;
import dev.twitchmod.rules.TempBlockManager;
import dev.twitchmod.state.Phase;
import dev.twitchmod.state.RunState;
import dev.twitchmod.state.StateManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The event director. Picks one record every 30–90 active seconds and enforces
 * the hard limits: one dangerous event at a time, no more than two chaos events
 * in a row, no unreachable tasks, no repeats in a short window, nothing that
 * drops the player into lava or the void.
 *
 * <p>An event can cost a reward, attention or a little health. It can never
 * hand out a strike by itself (spec §6).</p>
 */
public final class EventDirector {
    private static final Random RNG = new Random();
    private static EventContext active;
    private static final List<EventContext> FINISHED = new ArrayList<>();

    private EventDirector() {
    }

    public static EventContext active() {
        return active;
    }

    public static void tick(ServerPlayerEntity player, ServerWorld world) {
        RunState s = StateManager.get(world.getServer());
        if (!s.phase.randomEventsAllowed() || s.bRollMode) {
            if (active != null) finish(player, world, active);
            return;
        }

        Scheduler.tick(s.activeTicks);
        TempBlockManager.tick(s, world);

        if (active != null) {
            active.tick();
            if (active.result != EventContext.RUNNING || s.activeTicks >= active.endTick + 40L) {
                if (active.result == EventContext.RUNNING) active.fail();
                finish(player, world, active);
            }
            return;
        }
        if (s.nextEventTick < 0) {
            scheduleNext(s, 15);
            return;
        }
        if (s.activeTicks >= s.nextEventTick) tryStart(player, world, s);
    }

    public static void scheduleNext(RunState s, int inSeconds) {
        int min = TwitchModConfig.get().eventGapMinSeconds;
        int max = TwitchModConfig.get().eventGapMaxSeconds;
        int span = Math.max(1, max - min);
        int seconds = inSeconds > 0 ? inSeconds : min + RNG.nextInt(span);
        s.nextEventTick = s.activeTicks + seconds * 20L;
    }

    // ------------------------------------------------------------------ pick
    private static void tryStart(ServerPlayerEntity player, ServerWorld world, RunState s) {
        if (CutsceneDirector.isActive()) {
            scheduleNext(s, 6);
            return;
        }
        List<EventDefinition> pool = new ArrayList<>();
        int totalWeight = 0;
        for (EventDefinition d : EventCatalog.all()) {
            if (!feasible(d, player, s)) continue;
            pool.add(d);
            totalWeight += Math.max(1, d.weight);
        }
        if (pool.isEmpty() || totalWeight <= 0) {
            scheduleNext(s, 20);
            return;
        }
        int roll = RNG.nextInt(totalWeight);
        EventDefinition picked = pool.get(pool.size() - 1);
        for (EventDefinition d : pool) {
            roll -= Math.max(1, d.weight);
            if (roll < 0) {
                picked = d;
                break;
            }
        }
        start(player, world, s, picked);
    }

    private static boolean feasible(EventDefinition d, ServerPlayerEntity p, RunState s) {
        if (s.phase == Phase.GRACE && d.dangerous) return false;
        if (d.minActiveSec > s.activeSeconds()) return false;
        if (s.ticksSinceEventRan(d.id) < d.cooldownSec * 20L) return false;
        if (s.ticksSinceEventRan(d.id) < TwitchModConfig.get().sameEventRepeatBlockSeconds * 20L) return false;
        if (d.dangerous && s.activeDangerousEvents >= TwitchModConfig.get().maxDangerousEventsAtOnce) return false;
        if (d.category == EventCategory.CHAOS && s.chaosStreak >= TwitchModConfig.get().maxChaosInARow) return false;
        if (d.minHealth > 0 && (int) p.getHealth() < d.minHealth) return false;
        if (d.minArmorPieces > 0 && armorPieces(p) < d.minArmorPieces) return false;
        if (d.needsSafeEscape && s.activeDangerousEvents > 0) return false;
        if (!d.dimension.equals("overworld") && !p.getWorld().getRegistryKey().getValue().toString().endsWith(d.dimension)) {
            return false;
        }
        return true;
    }

    private static int armorPieces(ServerPlayerEntity p) {
        int n = 0;
        for (var st : p.getArmorItems()) if (!st.isEmpty()) n++;
        return n;
    }

    // ----------------------------------------------------------------- start
    public static boolean start(ServerPlayerEntity player, ServerWorld world, RunState s, String id) {
        EventDefinition d = EventCatalog.byId(id);
        if (d == null) return false;
        start(player, world, s, d);
        return true;
    }

    private static void start(ServerPlayerEntity player, ServerWorld world, RunState s, EventDefinition d) {
        EventContext ctx = new EventContext(d, player, world, s);
        active = ctx;
        s.currentEventId = d.id;
        s.currentEventStartTick = s.activeTicks;
        s.currentEventEndTick = ctx.endTick;
        s.currentEventState = 0;
        s.markEventRan(d.id);
        s.lastEventId = d.id;
        if (d.dangerous) {
            s.activeDangerousEvents++;
            if (d.category == EventCategory.CHAOS) s.chaosStreak++;
            else s.chaosStreak = 0;
        } else if (d.category != EventCategory.MINIGAME) {
            s.chaosStreak = 0;
        }
        AttentionManager.meaningful(s);

        ModNet.sendBanner(player, 0, "tm.event.start", 4, 0);
        player.sendMessage(Text.literal("§d" + d.title + "§7 — " + d.desc), false);
        if (!d.cutsceneId.isEmpty()) CutsceneDirector.play(player, d.cutsceneId);
        EditorLog.event(d.id, "START", false, s.strikesCurrent, s.activeSeconds(), d.category.key);

        EventEffects.start(ctx);
    }

    // ---------------------------------------------------------------- finish
    private static void finish(ServerPlayerEntity player, ServerWorld world, EventContext ctx) {
        RunState s = StateManager.get(world.getServer());
        boolean success = ctx.result == EventContext.SUCCESS;

        for (EventDefinition.Action a : success ? ctx.def.onSuccess : ctx.def.onFail) {
            EventEffects.fire(ctx, a);
        }
        if (success) {
            s.eventsCompleted++;
            ComboManager.onSuccess(s, player);
        } else {
            s.eventsFailed++;
            AttentionManager.add(s, -8);
        }
        if (ctx.def.dangerous) s.activeDangerousEvents = Math.max(0, s.activeDangerousEvents - 1);

        cleanup(ctx, world, s);
        EditorLog.event(ctx.def.id, success ? "SUCCESS" : "FAIL", false, s.strikesCurrent, s.activeSeconds(), "");
        ModNet.sendBanner(player, success ? 2 : 3, success ? "tm.event.success" : "tm.event.fail", 3, 0);

        s.currentEventId = "";
        s.currentEventState = success ? 1 : 2;
        active = null;
        scheduleNext(s, 0);
    }

    /** Called on death, dimension change, ban and world unload. */
    public static void abort(ServerPlayerEntity player, ServerWorld world, String reason) {
        if (active == null) return;
        EventContext ctx = active;
        ctx.fail();
        cleanup(ctx, world, StateManager.get(world.getServer()));
        active = null;
        EditorLog.event(ctx.def.id, "ABORT", false, 0, 0, reason);
        scheduleNext(StateManager.get(world.getServer()), 20);
    }

    public static void cleanupAll(ServerWorld world, RunState s) {
        if (active != null) {
            cleanup(active, world, s);
            active = null;
        }
        Scheduler.clearAll();
        EventEffects.clearTracked();
        Choices.clear();
    }

    private static void cleanup(EventContext ctx, ServerWorld world, RunState s) {
        for (Entity e : ctx.aliveSpawned()) {
            if (e instanceof net.minecraft.entity.ItemEntity) continue; // gifts may be kept
            e.discard();
        }
        ctx.spawned.clear();
        Scheduler.cancel(ctx);
        Choices.expire(ctx.def.id);
        if (ctx.combatPermitted) CombatPermissions.tick(s);
        FINISHED.add(ctx);
        if (FINISHED.size() > 32) FINISHED.remove(0);
    }
}
