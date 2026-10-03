package dev.twitchmod.rules;

import dev.twitchmod.state.RunState;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Event-created temporary blocks (meteor craters, arenas, bridges).
 * Everything placed here is restored exactly, and the set is wiped on save so a
 * reload can never leave scars in the world.
 */
public final class TempBlockManager {
    private static final Map<BlockPos, Long> TTL = new HashMap<>();
    private static final Map<BlockPos, BlockState> ORIGINAL = new HashMap<>();
    private static final List<BlockPos> ORDER = new ArrayList<>();

    private TempBlockManager() {
    }

    public static void place(ServerWorld world, BlockPos pos, BlockState state, int seconds) {
        if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return;
        if (!ORIGINAL.containsKey(pos)) {
            ORIGINAL.put(pos, world.getBlockState(pos));
            ORDER.add(pos);
        }
        world.setBlockState(pos, state, 3);
        TTL.put(pos, world.getServer().getTicks() + seconds * 20L);
    }

    public static void tick(RunState s, ServerWorld world) {
        if (TTL.isEmpty()) return;
        long now = world.getServer().getTicks();
        Iterator<BlockPos> it = ORDER.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            Long ttl = TTL.get(pos);
            if (ttl == null) {
                it.remove();
                continue;
            }
            if (ttl <= now) {
                restore(world, pos);
                it.remove();
            }
        }
        if (s != null) {
            s.tempBlocks.clear();
            for (BlockPos p : ORDER) s.tempBlocks.add(key(p));
        }
    }

    public static boolean isTemporary(BlockPos pos) {
        return TTL.containsKey(pos);
    }

    public static void restore(ServerWorld world, BlockPos pos) {
        BlockState st = ORIGINAL.getOrDefault(pos, Blocks.AIR.getDefaultState());
        world.setBlockState(pos, st, 3);
        ORIGINAL.remove(pos);
        TTL.remove(pos);
    }

    public static void restoreAll(ServerWorld world, RunState s) {
        for (BlockPos pos : new ArrayList<>(ORDER)) restore(world, pos);
        ORDER.clear();
        TTL.clear();
        ORIGINAL.clear();
        if (s != null) s.tempBlocks.clear();
    }

    public static int activeCount() {
        return ORDER.size();
    }

    public static String key(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
