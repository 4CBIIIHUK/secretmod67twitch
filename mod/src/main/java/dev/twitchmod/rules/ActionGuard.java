package dev.twitchmod.rules;

import dev.twitchmod.director.EventEffects;
import dev.twitchmod.director.EventMetrics;
import dev.twitchmod.state.Phase;
import dev.twitchmod.state.RunState;
import dev.twitchmod.state.StateManager;
import dev.twitchmod.run.RunLifecycle;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FireChargeItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Single authority for "may the player do this?".
 *
 * <p>Every denied attempt is cancelled <em>before</em> damage or a block break,
 * so the player is never punished retroactively: the block survives, the hit
 * does not land, the sealed chest stays shut. Repeated attempts inside a short
 * window do not create a strike series (spec §4).</p>
 */
public final class ActionGuard {
    /** Same violation on the same target inside this window costs nothing extra. */
    private static final int DEDUPE_TICKS = 60;
    private static final Map<String, Long> LAST_DENY = new HashMap<>();

    private ActionGuard() {
    }

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register(ActionGuard::beforeBreak);
        PlayerBlockBreakEvents.AFTER.register(ActionGuard::afterBreak);
        AttackEntityCallback.EVENT.register(ActionGuard::onAttack);
        UseBlockCallback.EVENT.register(ActionGuard::onUseBlock);
        UseEntityCallback.EVENT.register(ActionGuard::onUseEntity);
        UseItemCallback.EVENT.register(ActionGuard::onUseItem);
    }

    private static boolean rulesApply(RunState s) {
        return s != null && s.phase.rulesArmed() && !s.bRollMode;
    }

    private static boolean dedupe(RunState s, String key) {
        long now = s.activeTicks;
        Long last = LAST_DENY.get(key);
        LAST_DENY.put(key, now);
        return last != null && now - last < DEDUPE_TICKS;
    }

    // ------------------------------------------------------------- break block
    private static boolean beforeBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
        if (world.isClient || !(player instanceof ServerPlayerEntity sp)) return true;
        RunState s = StateManager.get(world.getServer());
        if (!rulesApply(s)) return true;

        Block block = state.getBlock();
        boolean chestLike = block instanceof ChestBlock || block instanceof BarrelBlock
                || block instanceof ShulkerBoxBlock || block instanceof EnderChestBlock;

        if (chestLike && s.sealedChests.contains(TempBlockManager.key(pos))) {
            if (!dedupe(s, "chest:" + pos)) {
                RunLifecycle.strike(sp, "tm.strike.reason.chest");
            }
            return false;
        }

        WorldDamageTracker.prune(s);
        if (WorldDamageTracker.limitReached(s)) {
            if (!s.flagged("wd_strike")) {
                s.flag("wd_strike");
                RunLifecycle.strike(sp, "tm.strike.reason.damage");
                sp.sendMessage(Text.translatable("tm.worlddamage.blocked"), true);
            }
            return false;
        }

        ToolClass required = ToolClass.forBlock(state);
        if (required != ToolClass.HAND_OK && !ToolClass.handAllowed(state)) {
            boolean owned = s.placedBlocks.containsKey(TempBlockManager.key(pos));
            if (!owned && !required.matches(player.getMainHandStack())) {
                if (!dedupe(s, "tool:" + pos)) {
                    RunLifecycle.strike(sp, "tm.strike.reason.tool");
                    sp.sendMessage(Text.translatable("tm.worlddamage.blocked"), true);
                }
                return false;
            }
        }
        return true;
    }

    private static void afterBreak(World world, net.minecraft.entity.player.PlayerEntity player, BlockPos pos,
                                   BlockState state, @Nullable BlockEntity be) {
        if (world.isClient || !(player instanceof ServerPlayerEntity sp) || !(world instanceof ServerWorld sw)) return;
        RunState s = StateManager.get(world.getServer());
        if (!s.phase.rulesArmed() || s.bRollMode) return;
        if (TempBlockManager.isTemporary(pos)) return; // event-made blocks are free

        WorldDamageTracker.record(s);
        s.lastMeaningfulActionTick = s.activeTicks;
        s.placedBlocks.remove(TempBlockManager.key(pos));

        if (WorldDamageTracker.warnThresholdReached(s)) {
            s.warnedAtWorldDamage = true;
            RunLifecycle.notify(sp, 1, "tm.worlddamage.warn", 3, WorldDamageTracker.countInWindow(s));
        }
        TempBlockManager.tick(s, sw);
    }

    // ---------------------------------------------------------------- hit mob
    private static ActionResult onAttack(PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult hit) {
        if (world.isClient || !(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
        RunState s = StateManager.get(world.getServer());
        if (!rulesApply(s)) return ActionResult.PASS;
        if (CombatPermissions.isAllowed(s, world, player, entity)) {
            s.lastMeaningfulActionTick = s.activeTicks;
            EventEffects.noteCombatHit(entity);
            return ActionResult.PASS;
        }
        if (!dedupe(s, "hit:" + entity.getId())) {
            RunLifecycle.strike(sp, "tm.strike.reason.hit");
        }
        return ActionResult.FAIL;
    }

    // ------------------------------------------------------------- use block
    private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (world.isClient || !(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
        RunState s = StateManager.get(world.getServer());
        ItemStack stack = player.getStackInHand(hand);
        Block block = world.getBlockState(hit.getBlockPos()).getBlock();

        // Sealed structure chest: nothing is given out, one strike.
        if (s.phase.rulesArmed() && !s.bRollMode
                && s.sealedChests.contains(TempBlockManager.key(hit.getBlockPos()))
                && (block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock
                || block instanceof EnderChestBlock)) {
            if (!dedupe(s, "chest:" + hit.getBlockPos())) {
                RunLifecycle.strike(sp, "tm.strike.reason.chest");
            }
            return ActionResult.FAIL;
        }

        // Arson / TNT: player-initiated destruction is the only thing punished.
        if (s.phase.rulesArmed() && !s.bRollMode
                && (stack.getItem() instanceof FlintAndSteelItem || stack.getItem() instanceof FireChargeItem)) {
            if (block == Blocks.TNT) {
                RunLifecycle.doubleStrike(sp, "tm.strike.reason.tnt", "tm.strike.reason.tnt2");
                return ActionResult.FAIL;
            }
            if (block != Blocks.NETHERRACK) { // netherrack lighting is decoration, still no fire spread
                if (!dedupe(s, "fire")) {
                    RunLifecycle.strike(sp, "tm.strike.reason.fire");
                }
                return ActionResult.FAIL;
            }
        }

        // Track player-placed blocks so they may be removed with any tool.
        if (stack.getItem() instanceof BlockItem) {
            s.placedBlocks.put(TempBlockManager.key(hit.getBlockPos().offset(hit.getSide())), s.activeTicks);
        }
        return ActionResult.PASS;
    }

    // ------------------------------------------------------------- use entity
    private static ActionResult onUseEntity(PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult hit) {
        if (world.isClient || !(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
        RunState s = StateManager.get(world.getServer());
        ItemStack stack = player.getStackInHand(hand);
        if (s.phase.rulesArmed() && !s.bRollMode
                && entity instanceof CreeperEntity && stack.getItem() instanceof FlintAndSteelItem) {
            RunLifecycle.doubleStrike(sp, "tm.strike.reason.creeper", "tm.strike.reason.creeper2");
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    /** Debug helper used by /twitchmod checkblock. */
    public static String describe(BlockState state, boolean owned) {
        if (owned) return "owned: any tool";
        ToolClass t = ToolClass.forBlock(state);
        if (ToolClass.handAllowed(state)) return "hand allowed";
        return "needs " + t.key();
    }

    public static Phase currentPhase(RunState s) {
        return s == null ? Phase.INTRO : s.phase;
    }
}
