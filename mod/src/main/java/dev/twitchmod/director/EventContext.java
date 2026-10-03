package dev.twitchmod.director;

import dev.twitchmod.state.RunState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime instance of a running event: objective progress, spawned entities and
 * the final result. Cleanup always runs, even when the server stops mid-event.
 */
public class EventContext {
    public static final int RUNNING = -1;
    public static final int SUCCESS = 1;
    public static final int FAIL = 0;

    public final EventDefinition def;
    public final ServerPlayerEntity player;
    public final ServerWorld world;
    public final RunState state;
    public final long startTick;
    public final long endTick;

    public int progress;
    public int goal;
    public Vec3d targetPos;
    public int targetRadius;
    public final List<Entity> spawned = new ArrayList<>();
    public final List<BlockPos> markedBlocks = new ArrayList<>();
    public final Map<String, Object> data = new HashMap<>();
    public int result = RUNNING;
    public boolean combatPermitted;

    public EventContext(EventDefinition def, ServerPlayerEntity player, ServerWorld world, RunState state) {
        this.def = def;
        this.player = player;
        this.world = world;
        this.state = state;
        this.startTick = state.activeTicks;
        this.endTick = state.activeTicks + def.durationSec * 20L;
        this.goal = Math.max(1, def.objectiveAmount);
        if ("reach".equals(def.objective)) {
            this.targetPos = player.getPos();
            this.targetRadius = Math.max(3, def.objectiveAmount);
        }
    }

    public boolean isDangerous() {
        return def.dangerous;
    }

    public void tick() {
        if (result != RUNNING) return;
        switch (def.objective) {
            case "survive" -> {
                if (player.isDead()) fail();
                else if (state.activeTicks >= endTick) succeed();
            }
            case "none" -> {
                if (state.activeTicks >= endTick) succeed();
            }
            case "collect" -> {
                progress = countItems(def.objectiveTarget);
                if (progress >= goal) succeed();
                else if (state.activeTicks >= endTick) fail();
            }
            case "eat" -> {
                progress += EventMetrics.drainEats();
                if (progress >= goal) succeed();
                else if (state.activeTicks >= endTick) fail();
            }
            case "break" -> {
                progress += EventMetrics.drainBreaks();
                if (progress >= goal) succeed();
                else if (state.activeTicks >= endTick) fail();
            }
            case "hit" -> {
                progress += EventMetrics.drainHits();
                if (progress >= goal) succeed();
                else if (state.activeTicks >= endTick) fail();
            }
            case "stand_still" -> {
                Vec3d anchor = (Vec3d) data.computeIfAbsent("anchor", k -> player.getPos());
                if (anchor.squaredDistanceTo(player.getPos()) > 4.0D) {
                    fail();
                } else if (state.activeTicks >= endTick) {
                    succeed();
                }
            }
            case "reach" -> {
                progress = (int) targetPos.distanceTo(player.getPos());
                if (progress <= targetRadius) succeed();
                else if (state.activeTicks >= endTick) fail();
            }
            case "protect" -> {
                if (state.activeTicks >= endTick) succeed();
            }
            default -> {
                if (state.activeTicks >= endTick) succeed();
            }
        }
    }

    private int countItems(String itemId) {
        int n = 0;
        Identifier id = new Identifier(itemId);
        for (ItemStack st : player.getInventory().main) {
            if (!st.isEmpty() && Registries.ITEM.getId(st.getItem()).equals(id)) n += st.getCount();
        }
        return n;
    }

    public void succeed() {
        if (result == RUNNING) result = SUCCESS;
    }

    public void fail() {
        if (result == RUNNING) result = FAIL;
    }

    public void despawnAll() {
        for (Entity e : spawned) {
            if (e != null && e.isAlive()) e.discard();
        }
        spawned.clear();
    }

    /** Live entities of this event still around, used by cleanup rules. */
    public List<Entity> aliveSpawned() {
        List<Entity> out = new ArrayList<>();
        for (Entity e : spawned) if (e != null && e.isAlive()) out.add(e);
        return out;
    }

    public Box area(double radius) {
        return Box.of(player.getPos(), radius * 2, radius * 2, radius * 2);
    }

    public BlockPos basePos() {
        return player.getBlockPos();
    }
}
