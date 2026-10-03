package dev.twitchmod.client.cutscene;

import dev.twitchmod.TwitchModConfig;
import dev.twitchmod.client.ClientState;
import dev.twitchmod.client.hud.TwitchHud;
import dev.twitchmod.cutscene.CutsceneDefinition;
import dev.twitchmod.cutscene.CutsceneLibrary;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;

/**
 * Client side of a cutscene: letterbox, subtitles, camera path.
 * Reduced-motion mode flattens the orbit and removes shake; subtitles stay on.
 */
public final class ClientCutscenePlayer {
    private static CutsceneDefinition active;
    private static long startMs;
    private static long endMs;

    private ClientCutscenePlayer() {
    }

    public static void play(String id) {
        CutsceneDefinition def = CutsceneLibrary.byId(id);
        if (def == null) return;
        active = def;
        long now = Util.getMeasuringTimeMs();
        startMs = now;
        endMs = now + def.durationSec * 1000L;
    }

    public static void stop() {
        active = null;
        endMs = 0L;
    }

    public static boolean isActive() {
        return active != null && Util.getMeasuringTimeMs() < endMs;
    }

    public static boolean controlsCamera() {
        return isActive() && !"static".equals(active.camera);
    }

    public static CutsceneDefinition current() {
        return active;
    }

    public static float progress01() {
        if (active == null) return 1f;
        float span = Math.max(1f, endMs - startMs);
        float done = (Util.getMeasuringTimeMs() - startMs) / span;
        return Math.max(0f, Math.min(1f, done));
    }

    public static double orbitRadians() {
        if (active == null) return 0;
        boolean reduced = TwitchModConfig.get().reducedMotion;
        return switch (active.camera) {
            case "orbit" -> progress01() * (reduced ? 0.35D : 1.1D);
            case "crane" -> progress01() * (reduced ? 0.5D : 1.6D);
            case "zoom" -> 0.0D;
            default -> 0.0D;
        };
    }

    public static double heightOffset() {
        if (active == null) return 0;
        boolean reduced = TwitchModConfig.get().reducedMotion;
        double base = "anvil".equals(active.camera) ? 2.0D : 1.1D;
        return base + (reduced ? 0 : Math.sin(progress01() * Math.PI) * 0.9D);
    }

    public static double distance() {
        boolean reduced = TwitchModConfig.get().reducedMotion;
        double base = switch (active == null ? "static" : active.camera) {
            case "zoom" -> 4.2D;
            case "anvil" -> 5.5D;
            default -> 3.6D;
        };
        if ("zoom".equals(active == null ? "" : active.camera) && !reduced) {
            return base - progress01() * 1.6D;
        }
        return base;
    }

    public static float shakeRadians() {
        if (active == null || !active.shake || TwitchModConfig.get().reducedMotion) return 0f;
        return (float) (Math.sin(Util.getMeasuringTimeMs() / 40.0D) * 0.02D);
    }

    public static float[] lookAngles(Vec3d center, Vec3d eye) {
        Vec3d d = center.subtract(eye);
        double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(d.y, horizontal));
        return new float[]{yaw + shakeRadians() * 57.3f, pitch};
    }

    // -------------------------------------------------------------- rendering
    public static void render(DrawContext ctx, TextRenderer tr, int w, int h) {
        if (!isActive()) return;
        boolean reduced = TwitchModConfig.get().reducedMotion;
        int bar = reduced ? h / 14 : h / 9;
        ctx.fill(0, 0, w, bar, 0xEE000000);
        ctx.fill(0, h - bar, w, h, 0xEE000000);

        // scene title
        if (active.title != null && !active.title.isEmpty()) {
            int tw = tr.getWidth(active.title);
            ctx.drawTextWithShadow(tr, active.title, w / 2 - tw / 2, bar + 6, TwitchHud.PURPLE);
        }
        // subtitles
        if (TwitchModConfig.get().subtitles) {
            int second = (int) ((Util.getMeasuringTimeMs() - startMs) / 1000L);
            String line = active.lineAt(second);
            if (line != null && !line.isEmpty()) {
                int lw = tr.getWidth(line);
                ctx.fill(w / 2 - lw / 2 - 5, h - bar - 22, w / 2 + lw / 2 + 5, h - bar - 6, 0x99000000);
                ctx.drawTextWithShadow(tr, line, w / 2 - lw / 2, h - bar - 19, TwitchHud.WHITE);
            }
        }
        // flash (disabled in reduced-flash mode)
        if (active.flash && !TwitchModConfig.get().reducedFlashes) {
            float p = progress01();
            if (p < 0.18f) {
                int alpha = (int) (150 * (1f - p / 0.18f));
                ctx.fill(0, 0, w, h, (alpha << 24) | 0xFF4444);
            }
        }
        // the strike counter is never hidden by a glitch effect
        String strikes = Text.translatable("tm.strike.count", ClientState.strikesCurrent + "/3").getString();
        ctx.drawTextWithShadow(tr, strikes, w - tr.getWidth(strikes) - 10, bar + 6, TwitchHud.WHITE);
    }
}
