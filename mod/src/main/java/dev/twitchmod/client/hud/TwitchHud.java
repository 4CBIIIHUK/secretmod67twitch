package dev.twitchmod.client.hud;

import dev.twitchmod.TwitchModConfig;
import dev.twitchmod.client.ClientState;
import dev.twitchmod.client.cutscene.ClientCutscenePlayer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/**
 * HUD priority (spec §10): real strikes, current goal and next event are always
 * visible. Colour is never the only carrier of meaning — every colour comes with
 * text and an icon.
 */
public final class TwitchHud {
    public static final int PURPLE = 0xFF9146FF;
    public static final int RED = 0xFFFF4444;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int GREEN = 0xFF44DD88;
    public static final int ORANGE = 0xFFFFAA33;
    public static final int BLACK = 0xCC000000;

    private TwitchHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(TwitchHud::render);
    }

    private static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        drawTopRight(ctx, tr, w);
        drawTopLeft(ctx, tr);
        drawBottomRight(ctx, tr, w, h);
        drawCrosshairVerdict(ctx, tr, w, h);
        drawPermit(ctx, tr, w, h);
        drawBanner(ctx, tr, w);
        ClientCutscenePlayer.render(ctx, tr, w, h);
    }

    private static void drawTopRight(DrawContext ctx, TextRenderer tr, int w) {
        int x = w - 8;
        int y = 8;
        // strikes: three blocks, real state only
        String label = Text.translatable("tm.strike.count", ClientState.strikesCurrent + "/3").getString();
        int lw = tr.getWidth(label);
        ctx.fill(x - lw - 6, y - 2, x, y + 12, BLACK);
        ctx.drawTextWithShadow(tr, label, x - lw - 3, y, colorForStrikes());
        for (int i = 0; i < 3; i++) {
            int bx = x - lw - 24 - i * 10;
            int color = i < ClientState.strikesCurrent ? RED : 0x55FFFFFF;
            ctx.fill(bx, y + 14, bx + 8, y + 22, color);
            ctx.drawTextWithShadow(tr, i < ClientState.strikesCurrent ? "X" : "-", bx + 2, y + 13, WHITE);
        }
        y += 28;
        String combo = Text.translatable("tm.combo.name") + ": " + ClientState.combo;
        ctx.fill(x - tr.getWidth(combo) - 6, y - 2, x, y + 11, BLACK);
        ctx.drawTextWithShadow(tr, combo, x - tr.getWidth(combo) - 3, y, PURPLE);
        y += 15;
        String perfect = ClientState.perfectEligible
                ? Text.translatable("tm.perfect.on").getString()
                : Text.translatable("tm.strike.count", ClientState.strikesTotal).getString();
        ctx.fill(x - tr.getWidth(perfect) - 6, y - 2, x, y + 11, BLACK);
        ctx.drawTextWithShadow(tr, perfect, x - tr.getWidth(perfect) - 3, y,
                ClientState.perfectEligible ? GREEN : 0xFF999999);
    }

    private static int colorForStrikes() {
        return switch (ClientState.strikesCurrent) {
            case 0 -> WHITE;
            case 1 -> ORANGE;
            default -> RED;
        };
    }

    private static void drawTopLeft(DrawContext ctx, TextRenderer tr) {
        int x = 8;
        int y = 8;
        String name = Text.translatable("tm.attention.name").getString();
        ctx.drawTextWithShadow(tr, name, x, y, PURPLE);
        int barW = 120;
        ctx.fill(x, y + 11, x + barW, y + 16, 0x66000000);
        int fill = (int) (barW * (ClientState.attention / 100.0D));
        ctx.fill(x, y + 11, x + fill, y + 16, ClientState.attention < 25 ? RED : PURPLE);
        ctx.drawTextWithShadow(tr, String.valueOf(ClientState.attention), x + barW + 4, y + 10, WHITE);
        if (ClientState.worldDamage > 0) {
            String wd = Text.translatable("tm.worlddamage.warn").getString().split(":")[0]
                    + ": " + ClientState.worldDamage + "/" + ClientState.worldDamageLimit;
            ctx.drawTextWithShadow(tr, wd, x, y + 22, ClientState.worldDamage > 120 ? ORANGE : WHITE);
        }
        if (!ClientState.taskKey.isEmpty()) {
            String task = Text.translatable("tm.phase.grace").getString();
            if (ClientState.phase == 1 && ClientState.graceSecondsLeft > 0) {
                task = Text.translatable("tm.grace.countdown", ClientState.graceSecondsLeft).getString();
            } else if (ClientState.phase == 3) {
                task = Text.translatable("tm.end.rush", ClientState.endRushSecondsLeft).getString();
            }
            ctx.drawTextWithShadow(tr, task, x, y + 34, ClientState.phase == 1 ? ORANGE : WHITE);
        }
    }

    private static void drawBottomRight(DrawContext ctx, TextRenderer tr, int w, int h) {
        int x = w - 8;
        int y = h - 34;
        String next = Text.translatable("tm.next.event", ClientState.secondsToNextEvent).getString();
        int nw = tr.getWidth(next);
        ctx.fill(x - nw - 6, y - 2, x, y + 11, BLACK);
        ctx.drawTextWithShadow(tr, next, x - nw - 3, y, PURPLE);
        y -= 15;
        String phase = switch (ClientState.phase) {
            case 1 -> Text.translatable("tm.grace.countdown", ClientState.graceSecondsLeft).getString();
            case 3 -> Text.translatable("tm.end.rush", ClientState.endRushSecondsLeft).getString();
            case 2 -> Text.translatable("tm.phase.active").getString();
            default -> Text.translatable("tm.phase.intro").getString();
        };
        int pw = tr.getWidth(phase);
        ctx.fill(x - pw - 6, y - 2, x, y + 11, BLACK);
        ctx.drawTextWithShadow(tr, phase, x - pw - 3, y, ClientState.phase == 3 ? RED : ORANGE);
    }

    private static void drawCrosshairVerdict(DrawContext ctx, TextRenderer tr, int w, int h) {
        if (ClientState.crosshairKey.isEmpty()) return;
        boolean ok = ClientState.crosshairKey.equals("tm.crosshair.ok");
        String text = Text.translatable(ClientState.crosshairKey).getString();
        int color = ok ? GREEN : RED;
        if (!ok) text = "! " + text;
        int tw = tr.getWidth(text);
        ctx.fill(w / 2 - tw / 2 - 3, h / 2 + 16, w / 2 + tw / 2 + 3, h / 2 + 28, BLACK);
        ctx.drawTextWithShadow(tr, text, w / 2 - tw / 2, h / 2 + 18, color);
    }

    private static void drawPermit(DrawContext ctx, TextRenderer tr, int w, int h) {
        if (!ClientState.hasPermit()) return;
        String text = Text.translatable("tm.crosshair.mobok", 0).getString() + " " + ClientState.permitLabel
                + " — " + Text.translatable("tm.combat.left", ClientState.permitSecondsLeft).getString();
        int tw = tr.getWidth(text);
        ctx.fill(w / 2 - tw / 2 - 4, h - 64, w / 2 + tw / 2 + 4, h - 50, BLACK);
        ctx.drawTextWithShadow(tr, text, w / 2 - tw / 2, h - 62, GREEN);
    }

    private static void drawBanner(DrawContext ctx, TextRenderer tr, int w) {
        ClientState.Banner b = ClientState.banner;
        if (b == null || Util.getMeasuringTimeMs() > b.expireAt) return;
        boolean fake = b.kind == 3;
        String title = switch (b.kind) {
            case 0 -> "! " + Text.translatable("tm.warn.title").getString() + " !";
            case 2 -> "+ " + Text.translatable("tm.event.success").getString();
            case 3 -> "? " + Text.translatable("tm.chat.simulated").getString();
            default -> "i";
        };
        String body = Text.translatable(b.key, toObjects(b.args)).getString();
        int tw = Math.max(tr.getWidth(title), tr.getWidth(body));
        boolean flashOn = !TwitchModConfig.get().reducedFlashes && (Util.getMeasuringTimeMs() / 120L) % 2L == 0L;
        int bg = b.kind == 0 ? (flashOn ? 0x88FF4444 : 0xAA220000) : BLACK;
        int x = w / 2 - tw / 2 - 8;
        int y = (int) (ctx.getScaledWindowHeight() * 0.28D);
        ctx.fill(x, y, x + tw + 16, y + 34, bg);
        ctx.drawTextWithShadow(tr, title, w / 2 - tr.getWidth(title) / 2, y + 3,
                b.kind == 0 ? RED : b.kind == 2 ? GREEN : fake ? ORANGE : PURPLE);
        ctx.drawTextWithShadow(tr, body, w / 2 - tr.getWidth(body) / 2, y + 18, WHITE);
    }

    private static Object[] toObjects(int[] args) {
        Object[] out = new Object[args == null ? 0 : args.length];
        for (int i = 0; i < out.length; i++) out[i] = args[i];
        return out;
    }
}
