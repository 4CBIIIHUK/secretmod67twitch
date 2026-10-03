package dev.twitchmod.client;

import dev.twitchmod.net.ModNet;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;

/** Client receiver: fills ClientState, never touches server authority. */
public final class ClientNet {
    private ClientNet() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ModNet.HUD, (client, handler, buf, rs) -> {
            Hud h = readHud(buf);
            client.execute(() -> {
                ClientState.phase = h.phase;
                ClientState.outcome = h.outcome;
                ClientState.strikesCurrent = h.strikesCurrent;
                ClientState.strikesTotal = h.strikesTotal;
                ClientState.combo = h.combo;
                ClientState.attention = h.attention;
                ClientState.secondsToNextEvent = h.secondsToNextEvent;
                ClientState.graceSecondsLeft = h.graceSecondsLeft;
                ClientState.endRushSecondsLeft = h.endRushSecondsLeft;
                ClientState.worldDamage = h.worldDamage;
                ClientState.worldDamageLimit = h.worldDamageLimit;
                ClientState.crosshairKey = h.crosshairKey;
                ClientState.permitLabel = h.permitLabel;
                ClientState.permitSecondsLeft = h.permitSecondsLeft;
                ClientState.perfectEligible = h.perfectEligible;
                ClientState.eventRunning = h.eventRunning;
                ClientState.taskKey = h.taskKey;
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNet.BANNER, (client, handler, buf, rs) -> {
            int kind = buf.readVarInt();
            String key = buf.readString(128);
            int seconds = buf.readVarInt();
            int n = buf.readVarInt();
            int[] args = new int[n];
            for (int i = 0; i < n; i++) args[i] = buf.readVarInt();
            client.execute(() -> {
                ClientState.Banner b = new ClientState.Banner();
                b.kind = kind;
                b.key = key;
                b.args = args;
                b.expireAt = System.currentTimeMillis() + Math.max(1, seconds) * 1000L;
                ClientState.banner = b;
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNet.CHOICE, (client, handler, buf, rs) -> {
            String id = buf.readString(128);
            String title = buf.readString(128);
            boolean simulated = buf.readBoolean();
            int timeout = buf.readVarInt();
            int count = buf.readVarInt();
            List<String> options = new ArrayList<>();
            List<Integer> percents = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                options.add(buf.readString(128));
                percents.add(buf.readVarInt());
            }
            long deadline = System.currentTimeMillis() + Math.max(3, timeout) * 1000L;
            client.execute(() -> {
                ClientState.Choice c = new ClientState.Choice();
                c.id = id;
                c.titleKey = title;
                c.simulated = simulated;
                c.optionKeys = options;
                c.percents = percents;
                c.expireAt = deadline;
                ClientState.choice = c;
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null && mc.currentScreen == null) {
                    mc.setScreen(new dev.twitchmod.client.ui.ChoiceScreen(c));
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNet.CUTSCENE, (client, handler, buf, rs) -> {
            String id = buf.readString(128);
            client.execute(() -> dev.twitchmod.client.cutscene.ClientCutscenePlayer.play(id));
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNet.CUTSCENE_STOP, (client, handler, buf, rs) ->
                client.execute(() -> dev.twitchmod.client.cutscene.ClientCutscenePlayer.stop()));

        ClientPlayNetworking.registerGlobalReceiver(ModNet.OUTCOME, (client, handler, buf, rs) -> {
            int outcome = buf.readVarInt();
            String title = buf.readString(128);
            String detail = buf.readString(128);
            int strikesTotal = buf.readVarInt();
            boolean perfect = buf.readBoolean();
            boolean allowObserve = buf.readBoolean();
            client.execute(() -> {
                ClientState.OutcomeScreenData d = new ClientState.OutcomeScreenData();
                d.outcome = outcome;
                d.titleKey = title;
                d.detailKey = detail;
                d.strikesTotal = strikesTotal;
                d.perfect = perfect;
                d.allowObserve = allowObserve;
                d.expireAt = System.currentTimeMillis() + 60_000L;
                ClientState.outcomeScreen = d;
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) mc.setScreen(new dev.twitchmod.client.ui.OutcomeScreen(d));
            });
        });
    }

    public static void sendChoicePick(String id, int index) {
        PacketByteBuf buf = dev.twitchmod.net.Bufs.create();
        buf.writeString(id, 128);
        buf.writeVarInt(index);
        ClientPlayNetworking.send(ModNet.CHOICE_PICK, buf);
    }

    public static void sendBanChoice(int index) {
        PacketByteBuf buf = dev.twitchmod.net.Bufs.create();
        buf.writeVarInt(index);
        ClientPlayNetworking.send(ModNet.BAN_CHOICE, buf);
    }

    private static Hud readHud(PacketByteBuf buf) {
        Hud h = new Hud();
        h.phase = buf.readVarInt();
        h.outcome = buf.readVarInt();
        h.strikesCurrent = buf.readVarInt();
        h.strikesTotal = buf.readVarInt();
        h.combo = buf.readVarInt();
        h.attention = buf.readVarInt();
        h.secondsToNextEvent = buf.readVarInt();
        h.graceSecondsLeft = buf.readVarInt();
        h.endRushSecondsLeft = buf.readVarInt();
        h.worldDamage = buf.readVarInt();
        h.worldDamageLimit = buf.readVarInt();
        h.crosshairKey = buf.readString(64);
        h.permitLabel = buf.readString(64);
        h.permitSecondsLeft = buf.readVarInt();
        h.perfectEligible = buf.readBoolean();
        h.eventRunning = buf.readBoolean();
        int args = buf.readVarInt();
        for (int i = 0; i < args; i++) buf.readVarInt();
        h.taskKey = buf.readString(128);
        return h;
    }

    private static class Hud {
        int phase;
        int outcome;
        int strikesCurrent;
        int strikesTotal;
        int combo;
        int attention;
        int secondsToNextEvent;
        int graceSecondsLeft;
        int endRushSecondsLeft;
        int worldDamage;
        int worldDamageLimit;
        String crosshairKey = "";
        String permitLabel = "";
        int permitSecondsLeft;
        boolean perfectEligible = true;
        boolean eventRunning;
        String taskKey = "";
    }
}
