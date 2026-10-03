package dev.twitchmod.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.twitchmod.TwitchMod;
import dev.twitchmod.TwitchModConfig;
import dev.twitchmod.attention.AttentionManager;
import dev.twitchmod.cutscene.CutsceneDirector;
import dev.twitchmod.cutscene.CutsceneLibrary;
import dev.twitchmod.director.EventCatalog;
import dev.twitchmod.director.EventDirector;
import dev.twitchmod.log.EditorLog;
import dev.twitchmod.rules.WorldDamageTracker;
import dev.twitchmod.run.EndRushManager;
import dev.twitchmod.run.GraceManager;
import dev.twitchmod.run.RunLifecycle;
import dev.twitchmod.state.Phase;
import dev.twitchmod.state.RunOutcome;
import dev.twitchmod.state.RunState;
import dev.twitchmod.state.StateManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Debug / production commands. Everything here is explicit: no command creates a
 * strike silently, and B-roll mode always marks the run as invalid for the board.
 */
public final class ModCommands {
    private ModCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(
                literal("twitchmod")
                        .then(literal("status").executes(ModCommands::status))
                        .then(literal("start").executes(ModCommands::start))
                        .then(literal("reset").executes(ModCommands::reset))
                        .then(literal("strike").then(argument("reason", StringArgumentType.string())
                                .executes(ModCommands::strike)))
                        .then(literal("amnesty").executes(ModCommands::amnesty))
                        .then(literal("grace").then(argument("seconds", StringArgumentType.string())
                                .executes(ModCommands::grace)))
                        .then(literal("event").then(argument("id", StringArgumentType.string())
                                .executes(ModCommands::event)))
                        .then(literal("events").executes(ctx -> listEvents(ctx, "")))
                        .then(literal("cutscene").then(argument("id", StringArgumentType.string())
                                .executes(ModCommands::cutscene)))
                        .then(literal("cutscenes").executes(ModCommands::listCutscenes))
                        .then(literal("broll").then(argument("on", StringArgumentType.string())
                                .executes(ModCommands::broll)))
                        .then(literal("seal").executes(ModCommands::seal))
                        .then(literal("export").then(argument("what", StringArgumentType.string())
                                .executes(ModCommands::export)))
                        .then(literal("reload").executes(ModCommands::reload))
        ));
    }

    private static RunState state(CommandContext<ServerCommandSource> ctx) {
        return StateManager.get(ctx.getSource().getServer());
    }

    private static ServerPlayerEntity player(CommandContext<ServerCommandSource> ctx) {
        return ctx.getSource().getPlayer();
    }

    private static int status(CommandContext<ServerCommandSource> ctx) {
        RunState s = state(ctx);
        ServerPlayerEntity p = player(ctx);
        ctx.getSource().sendFeedback(() -> Text.literal(
                "§dTwitchMod§7 v" + TwitchModConfig.class.getSimpleName()), false);
        ctx.getSource().sendFeedback(() -> Text.literal(
                "phase=" + s.phase + " outcome=" + s.outcome + " strikes=" + s.strikesCurrent + "/3 total="
                        + s.strikesTotal + " combo=" + s.combo + " attention=" + s.attention), false);
        ctx.getSource().sendFeedback(() -> Text.literal(
                "active=" + s.activeSeconds() + "s blocks=" + WorldDamageTracker.countInWindow(s) + "/"
                        + TwitchModConfig.get().worldDamageLimit + " events=" + EventCatalog.size()
                        + " cutscenes=" + CutsceneLibrary.size()), false);
        ctx.getSource().sendFeedback(() -> Text.literal(
                "next_event_in=" + Math.max(0, (s.nextEventTick - s.activeTicks) / 20) + "s current="
                        + (s.currentEventId.isEmpty() ? "-" : s.currentEventId)), false);
        if (p != null) ctx.getSource().sendFeedback(() -> Text.literal(
                "grace_left=" + (s.phase == Phase.GRACE ? GraceManager.secondsLeft(s) : 0)
                        + " end_rush_left=" + (s.phase == Phase.END_RUSH ? EndRushManager.secondsLeft(s) : 0)), false);
        List<String> problems = EventCatalog.problems();
        ctx.getSource().sendFeedback(() -> Text.literal("catalogue_problems=" + problems.size()), false);
        return 1;
    }

    private static int start(CommandContext<ServerCommandSource> ctx) {
        RunState s = state(ctx);
        ServerPlayerEntity p = player(ctx);
        if (p == null) return 0;
        s.phase = Phase.INTRO;
        s.outcome = RunOutcome.NONE;
        RunLifecycle.resetIntroFlag();
        RunLifecycle.onJoin(p, ctx.getSource().getServer());
        GraceManager.begin(s, p);
        return 1;
    }

    private static int reset(CommandContext<ServerCommandSource> ctx) {
        RunLifecycle.resetIntroFlag();
        RunState s = StateManager.reset(ctx.getSource().getServer());
        s.phase = Phase.INTRO;
        ServerPlayerEntity p = player(ctx);
        if (p != null) RunLifecycle.onJoin(p, ctx.getSource().getServer());
        ctx.getSource().sendFeedback(() -> Text.literal("§aНовый забег создан"), false);
        return 1;
    }

    private static int strike(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) return 0;
        String reason = StringArgumentType.getString(ctx, "reason");
        RunLifecycle.strike(p, reason.startsWith("tm.") ? reason : "tm.strike.reason." + reason);
        return 1;
    }

    private static int amnesty(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) return 0;
        RunLifecycle.applyAmnesty(p);
        return 1;
    }

    private static int grace(CommandContext<ServerCommandSource> ctx) {
        RunState s = state(ctx);
        int seconds = parseInt(StringArgumentType.getString(ctx, "seconds"),
                TwitchModConfig.get().graceSeconds);
        s.flags.put("grace_end", s.activeTicks + seconds * 20L);
        ctx.getSource().sendFeedback(() -> Text.literal("§aГрейс: " + seconds + " c"), false);
        return 1;
    }

    private static int event(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) return 0;
        String id = StringArgumentType.getString(ctx, "id");
        ServerWorld world = p.getServerWorld();
        boolean ok = EventDirector.start(p, world, StateManager.get(p.getServer()), id);
        ctx.getSource().sendFeedback(() -> Text.literal(ok ? "§aЗапущено: " + id : "§cНет события " + id), false);
        return 1;
    }

    private static int listEvents(CommandContext<ServerCommandSource> ctx, String filter) {
        ctx.getSource().sendFeedback(() -> Text.literal("§dСобытий: " + EventCatalog.size()), false);
        EventCatalog.counts().forEach((cat, n) -> ctx.getSource().sendFeedback(
                () -> Text.literal(" §7" + cat.key + " = " + n + "/" + cat.targetCount), false));
        List<String> problems = EventCatalog.problems();
        for (int i = 0; i < Math.min(10, problems.size()); i++) {
            String problem = problems.get(i);
            ctx.getSource().sendFeedback(() -> Text.literal("§c" + problem), false);
        }
        return 1;
    }

    private static int cutscene(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) return 0;
        String id = StringArgumentType.getString(ctx, "id");
        ctx.getSource().sendFeedback(() -> Text.literal(
                CutsceneDirector.play(p, id) ? "§aКатсцена: " + id : "§cНет катсцены " + id), false);
        return 1;
    }

    private static int listCutscenes(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> Text.literal("§dКатсцен: " + CutsceneLibrary.size()), false);
        CutsceneLibrary.countsByType().forEach((type, n) -> ctx.getSource().sendFeedback(
                () -> Text.literal(" §7" + type + " = " + n), false));
        return 1;
    }

    private static int broll(CommandContext<ServerCommandSource> ctx) {
        RunState s = state(ctx);
        ServerPlayerEntity p = player(ctx);
        boolean on = StringArgumentType.getString(ctx, "on").equalsIgnoreCase("on");
        s.bRollMode = on;
        if (on) {
            s.outcome = RunOutcome.INVALID_FOR_LEADERBOARD;
            s.perfectEligible = false;
        }
        EditorLog.marker("BROLL", on ? "ON" : "OFF", "MODE", false, s.strikesCurrent, s.activeSeconds(), "");
        if (p != null) p.sendMessage(Text.translatable("tm.obr.mode"), false);
        return 1;
    }

    private static int seal(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) return 0;
        dev.twitchmod.progress.StoryProgress.sealNearbyChest(p, p.getServerWorld());
        ctx.getSource().sendFeedback(() -> Text.literal("§aСундуки рядом помечены как запечатанные"), false);
        return 1;
    }

    private static int export(CommandContext<ServerCommandSource> ctx) {
        String what = StringArgumentType.getString(ctx, "what");
        Path dir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir()
                .resolve("logs").resolve("twitchmod");
        try {
            Files.createDirectories(dir);
            String stamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            if (what.equalsIgnoreCase("registry")) {
                Path out = dir.resolve("event_registry_" + stamp + ".csv");
                Files.writeString(out, EventCatalog.exportCsv(), StandardCharsets.UTF_8);
                ctx.getSource().sendFeedback(() -> Text.literal("§aРеестр: " + out), false);
            } else {
                Path out = dir.resolve("editor_log_export_" + stamp + ".csv");
                StringBuilder sb = new StringBuilder("time,kind,id,outcome,notification,strikes,active_s,detail\n");
                EditorLog.tail(4000).forEach(l -> sb.append(l).append('\n'));
                Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
                ctx.getSource().sendFeedback(() -> Text.literal("§aЛог: " + out), false);
            }
        } catch (Exception e) {
            TwitchMod.LOGGER.warn("[twitchmod] export failed: {}", e.toString());
            ctx.getSource().sendError(Text.literal("export failed"));
        }
        return 1;
    }

    private static int reload(CommandContext<ServerCommandSource> ctx) {
        TwitchModConfig.reload();
        AttentionManager.meaningful(state(ctx));
        ctx.getSource().sendFeedback(() -> Text.literal("§aКонфиг перечитан, тик " + Util.getMeasuringTimeMs()), false);
        return 1;
    }
}
