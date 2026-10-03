package dev.twitchmod.progress;

import dev.twitchmod.net.ModNet;
import dev.twitchmod.state.RunState;
import dev.twitchmod.state.RunStateHolder;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

/**
 * Story guarantees. The player may always do everything the vanilla way; these
 * are the safety net for an unlucky or over-long run (spec §7). Nothing here is
 * mandatory and nothing here hands out a strike.
 */
public final class StoryProgress {
    private StoryProgress() {
    }

    public static void tick(ServerPlayerEntity p, ServerWorld w) {
        RunState s = RunStateHolder.get(w);
        if (s == null || s.phase.isEndgame() || s.bRollMode) return;
        long sec = s.activeSeconds();

        if (sec >= 1800 && s.storyStage < 1) {
            s.storyStage = 1;
            ModNet.sendBanner(p, 1, "tm.story.stronghold", 8, 0);
            describe(p, w, false);
        }
        if (sec >= 2400 && s.storyStage < 2) {
            s.storyStage = 2;
            describe(p, w, true);
        }
        if (sec >= 3300 && s.storyStage < 3) {
            s.storyStage = 3;
            giveMissingEyes(p, 12);
        }
        if (sec >= 4200 && s.storyStage < 4) {
            s.storyStage = 4;
            offerFastTravel(p, w);
        }
        if (!s.endKitChecked && w.getRegistryKey().getValue().toString().equals("minecraft:the_end")) {
            ensureMinimalKit(p);
        }
    }

    /** Story event handler used by the catalogue. */
    public static void handle(ServerPlayerEntity p, ServerWorld w, String id) {
        RunState s = RunStateHolder.get(w);
        switch (id == null ? "" : id) {
            case "story_air_start", "story_grace_end", "story_portal_found", "story_end_voice",
                 "story_portal_hint", "story_dragon_brief" -> ModNet.sendBanner(p, 1, "tm.event.start", 4, 0);
            case "story_stronghold_hint" -> describe(p, w, false);
            case "story_stronghold_coords" -> describe(p, w, true);
            case "story_eyes_task", "story_eye_market" -> giveMissingEyes(p, 12);
            case "story_blaze_hint" -> {
                p.getInventory().insertStack(new ItemStack(Items.BLAZE_ROD, 3));
                p.playerScreenHandler.sendContentUpdates();
            }
            case "story_nether_beacon" -> ModNet.sendBanner(p, 1, "tm.story.stronghold", 6, 0);
            case "story_fortress_reveal" -> describe(p, w, true);
            case "story_kit_check" -> ensureMinimalKit(p);
            case "story_fast_travel" -> offerFastTravel(p, w);
            case "story_teacher", "story_final_exam", "story_first_tool", "story_craft_hint",
                 "story_food_supply" -> ensureMinimalKit(p);
            default -> ModNet.sendBanner(p, 2, "tm.event.success", 3, 0);
        }
        if (s != null && id != null && !id.isEmpty()) s.flag(id);
    }

    // ------------------------------------------------------------- helpers
    public static BlockPos stronghold(MinecraftServer server) {
        ServerWorld over = server.getOverworld();
        BlockPos from = over.getSpawnPos();
        BlockPos found = over.locateStructure(StructureTags.EYE_OF_ENDER_LOCATED, from, 64, false);
        return found == null ? from : found;
    }

    private static void describe(ServerPlayerEntity p, ServerWorld w, boolean exact) {
        if (w.getServer() == null) return;
        BlockPos target = stronghold(w.getServer());
        BlockPos from = p.getBlockPos();
        int dx = target.getX() - from.getX();
        int dz = target.getZ() - from.getZ();
        Direction.Axis axisX = dx >= 0 ? Direction.Axis.X : Direction.Axis.X;
        int adx = Math.abs(dx);
        int adz = Math.abs(dz);
        String dir = adx > adz ? (dx > 0 ? "восток" : "запад") : (dz > 0 ? "юг" : "север");
        if (exact) {
            p.sendMessage(Text.literal("§5Модерация: крепость ~ X=" + target.getX() + " Y=" + target.getY()
                    + " Z=" + target.getZ()), false);
        } else {
            p.sendMessage(Text.literal("§5Модерация: сигнал идёт на " + dir + ", примерно "
                    + Math.max(adx, adz) + " блоков."), false);
        }
        p.sendMessage(Text.literal("§7(" + axisX.getName() + " смещение " + dx + ", " + dz + ")"), false);
    }

    public static void giveMissingEyes(ServerPlayerEntity p, int target) {
        int have = 0;
        for (ItemStack st : p.getInventory().main) if (st.getItem() == Items.ENDER_EYE) have += st.getCount();
        int missing = target - have;
        if (missing > 0) {
            p.getInventory().insertStack(new ItemStack(Items.ENDER_EYE, missing));
            p.playerScreenHandler.sendContentUpdates();
        }
        ModNet.sendBanner(p, 2, "tm.story.stronghold", 5, target);
    }

    /** Voluntary, announced transfer — never forced, never into lava or void. */
    public static void offerFastTravel(ServerPlayerEntity p, ServerWorld w) {
        RunState s = RunStateHolder.get(w);
        if (s == null || s.fastTravelOffered) return;
        s.fastTravelOffered = true;
        ModNet.sendChoice(p, "story_fast_travel", "tm.story.fasttravel", false, 20,
                List.of("tm.choice.opt.yes", "tm.choice.opt.no"), List.of(50, 50));
        dev.twitchmod.director.Choices.offer("story_fast_travel", index -> {
            if (index == 0 && w.getServer() != null) fastTravel(p, w.getServer());
        });
    }

    public static void fastTravel(ServerPlayerEntity p, MinecraftServer server) {
        RunState s = RunStateHolder.get(server.getOverworld());
        BlockPos target = stronghold(server);
        ServerWorld over = server.getOverworld();
        int y = over.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, target.getX(), target.getZ());
        p.teleport(over, target.getX() + 0.5D, y + 1.0D, target.getZ() + 0.5D, p.getYaw(), p.getPitch());
        p.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.SLOW_FALLING, 20 * 20, 0));
        if (s != null) {
            s.fastTravelUsed = true;
            dev.twitchmod.log.EditorLog.marker("STORY", "fast_travel", "USED", false, s.strikesCurrent, s.activeSeconds(), "");
        }
    }

    /** Minimal viable dragon kit — never a netherite set, never 20 beds. */
    public static void ensureMinimalKit(ServerPlayerEntity p) {
        if (has(p, Items.IRON_SWORD) && has(p, Items.IRON_PICKAXE) && count(p, Items.COOKED_BEEF) >= 4) return;
        give(p, new ItemStack(Items.IRON_SWORD));
        give(p, new ItemStack(Items.IRON_PICKAXE));
        give(p, new ItemStack(Items.COOKED_BEEF, 8));
        give(p, new ItemStack(Items.COBBLESTONE, 16));
        give(p, new ItemStack(Items.TORCH, 8));
        p.playerScreenHandler.sendContentUpdates();
        ModNet.sendBanner(p, 2, "tm.event.success", 3, 0);
    }

    private static boolean has(ServerPlayerEntity p, net.minecraft.item.Item item) {
        for (ItemStack st : p.getInventory().main) if (st.getItem() == item) return true;
        return false;
    }

    private static int count(ServerPlayerEntity p, net.minecraft.item.Item item) {
        int n = 0;
        for (ItemStack st : p.getInventory().main) if (st.getItem() == item) n += st.getCount();
        return n;
    }

    private static void give(ServerPlayerEntity p, ItemStack st) {
        p.getInventory().insertStack(st);
    }

    /** Marks a sealed structure chest found near the player (rule demo tool). */
    public static void sealNearbyChest(ServerPlayerEntity p, ServerWorld w) {
        BlockPos base = p.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(base.add(-6, -3, -6), base.add(6, 4, 6))) {
            if (w.getBlockState(pos).isOf(Blocks.CHEST)) {
                RunState s = RunStateHolder.get(w);
                if (s != null) s.sealedChests.add(pos.getX() + "," + pos.getY() + "," + pos.getZ());
            }
        }
    }
}
