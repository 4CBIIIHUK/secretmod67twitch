package dev.twitchmod.log;

import dev.twitchmod.TwitchMod;
import dev.twitchmod.TwitchModConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Editor log (spec §11): one flat, greppable stream with everything a cutter needs.
 * Marked rows: events, outcomes, real vs fake notifications, strikes, cutscene
 * start/end, big rewards, End entry, finale.
 */
public final class EditorLog {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final List<String> buffer = new ArrayList<>();
    private static Path csv;
    private static Path jsonl;

    private EditorLog() {
    }

    public static void init() {
        Path dir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("logs").resolve("twitchmod");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            TwitchMod.LOGGER.warn("[twitchmod] cannot create editor log dir: {}", e.toString());
            return;
        }
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        csv = dir.resolve("editor_log_" + stamp + ".csv");
        jsonl = dir.resolve("editor_log_" + stamp + ".jsonl");
        write("time,kind,id,outcome,notification,strikes,active_s,detail\n", csv);
        TwitchMod.LOGGER.info("[twitchmod] editor log: {}", csv);
    }

    public static synchronized void marker(String kind, String id, String outcome, boolean fake, int strikes, long activeS, String detail) {
        if (!TwitchModConfig.get().editorLogEnabled) return;
        String line = String.join(",",
                LocalDateTime.now().format(TS),
                kind, safe(id), safe(outcome),
                fake ? "FAKE" : "REAL",
                String.valueOf(strikes), String.valueOf(activeS), safe(detail));
        buffer.add(line);
        write(line + "\n", csv);
        if (TwitchModConfig.get().editorLogJson) {
            write("{\"time\":\"" + LocalDateTime.now() + "\",\"kind\":\"" + kind + "\",\"id\":\"" + safe(id)
                    + "\",\"outcome\":\"" + safe(outcome) + "\",\"notification\":\"" + (fake ? "FAKE" : "REAL")
                    + "\",\"strikes\":" + strikes + ",\"active_s\":" + activeS + ",\"detail\":\"" + safe(detail) + "\"}\n", jsonl);
        }
        if (buffer.size() > 500) buffer.subList(0, 200).clear();
    }

    public static void event(String id, String outcome, boolean fake, int strikes, long activeS, String detail) {
        marker("EVENT", id, outcome, fake, strikes, activeS, detail);
    }

    public static void cutscene(String id, boolean start, int strikes, long activeS) {
        marker("CUTSCENE", id, start ? "START" : "END", false, strikes, activeS, "");
    }

    public static void strike(String reason, int count, long activeS) {
        marker("STRIKE", reason, "WARN", false, count, activeS, "count=" + count);
    }

    public static void reward(String id, String table, long activeS) {
        marker("REWARD", id, table, false, 0, activeS, "");
    }

    public static void phase(String phase, long activeS) {
        marker("PHASE", phase, "START", false, 0, activeS, "");
    }

    public static void outcome(String outcome, long activeS, String detail) {
        marker("OUTCOME", outcome, "END", false, 0, activeS, detail);
    }

    public static List<String> tail(int lines) {
        int from = Math.max(0, buffer.size() - lines);
        return new ArrayList<>(buffer.subList(from, buffer.size()));
    }

    public static Path currentCsv() {
        return csv;
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace(',', ';').replace('\n', ' ').replace('"', '\'');
    }

    private static void write(String content, Path target) {
        if (target == null) return;
        try {
            Files.writeString(target, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Never let logging break a run.
        }
    }
}
