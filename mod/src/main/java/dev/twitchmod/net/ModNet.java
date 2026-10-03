package dev.twitchmod.net;

import dev.twitchmod.TwitchMod;
import dev.twitchmod.director.Choices;
import dev.twitchmod.state.RunOutcome;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Server → client presentation channel and the two client answers the UI may send.
 * A pretty client effect can never create or cancel a strike: it only draws packets.
 */
public final class ModNet {
    public static final Identifier HUD = TwitchMod.id("hud");
    public static final Identifier BANNER = TwitchMod.id("banner");
    public static final Identifier CHOICE = TwitchMod.id("choice");
    public static final Identifier CHOICE_PICK = TwitchMod.id("choice_pick");
    public static final Identifier CUTSCENE = TwitchMod.id("cutscene");
    public static final Identifier CUTSCENE_STOP = TwitchMod.id("cutscene_stop");
    public static final Identifier OUTCOME = TwitchMod.id("outcome");
    public static final Identifier BAN_CHOICE = TwitchMod.id("ban_choice");

    private ModNet() {
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(CHOICE_PICK, (server, player, handler, buf, rs) -> {
            String id = buf.readString(256);
            int index = buf.readVarInt();
            server.execute(() -> Choices.resolve(id, index));
        });
        ServerPlayNetworking.registerGlobalReceiver(BAN_CHOICE, (server, player, handler, buf, rs) -> {
            int index = buf.readVarInt();
            server.execute(() -> dev.twitchmod.run.RunLifecycle.onBanChoice(player, index));
        });
    }

    // ------------------------------------------------------------------ hud
    public static void sendHud(ServerPlayerEntity p, HudSnapshot s) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(s.phase);
        buf.writeVarInt(s.outcome);
        buf.writeVarInt(s.strikesCurrent);
        buf.writeVarInt(s.strikesTotal);
        buf.writeVarInt(s.combo);
        buf.writeVarInt(s.attention);
        buf.writeVarInt(s.secondsToNextEvent);
        buf.writeVarInt(s.graceSecondsLeft);
        buf.writeVarInt(s.endRushSecondsLeft);
        buf.writeVarInt(s.worldDamage);
        buf.writeVarInt(s.worldDamageLimit);
        buf.writeString(s.crosshairKey, 64);
        buf.writeString(s.permitLabel, 64);
        buf.writeVarInt(s.permitSecondsLeft);
        buf.writeBoolean(s.perfectEligible);
        buf.writeBoolean(s.eventRunning);
        buf.writeVarInt(s.taskArgs.length);
        for (int a : s.taskArgs) buf.writeVarInt(a);
        buf.writeString(s.taskKey, 128);
        ServerPlayNetworking.send(p, HUD, buf);
    }

    // --------------------------------------------------------------- banner
    public static void sendBanner(ServerPlayerEntity p, int kind, String key, int seconds, int... args) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(kind);
        buf.writeString(key, 128);
        buf.writeVarInt(seconds);
        buf.writeVarInt(args.length);
        for (int a : args) buf.writeVarInt(a);
        ServerPlayNetworking.send(p, BANNER, buf);
    }

    // ---------------------------------------------------------------- choice
    public static void sendChoice(ServerPlayerEntity p, String id, String titleKey, boolean simulated,
                                  int timeoutSec, List<String> optionKeys, List<Integer> percents) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(id, 128);
        buf.writeString(titleKey, 128);
        buf.writeBoolean(simulated);
        buf.writeVarInt(timeoutSec);
        buf.writeVarInt(optionKeys.size());
        for (int i = 0; i < optionKeys.size(); i++) {
            buf.writeString(optionKeys.get(i), 128);
            buf.writeVarInt(percents.size() > i ? percents.get(i) : 0);
        }
        ServerPlayNetworking.send(p, CHOICE, buf);
    }

    // -------------------------------------------------------------- cutscene
    public static void sendCutscene(ServerPlayerEntity p, String id) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(id, 128);
        ServerPlayNetworking.send(p, CUTSCENE, buf);
    }

    public static void sendCutsceneStop(ServerPlayerEntity p) {
        ServerPlayNetworking.send(p, CUTSCENE_STOP, PacketByteBufs.create());
    }

    // --------------------------------------------------------------- outcome
    public static void sendOutcome(ServerPlayerEntity p, RunOutcome outcome, String titleKey, String detailKey,
                                   int strikesTotal, boolean perfect, boolean allowObserve) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(outcome.ordinal());
        buf.writeString(titleKey, 128);
        buf.writeString(detailKey, 128);
        buf.writeVarInt(strikesTotal);
        buf.writeBoolean(perfect);
        buf.writeBoolean(allowObserve);
        ServerPlayNetworking.send(p, OUTCOME, buf);
    }

    /** Crosshair verdict keys, see client HUD legend. */
    public static final String OK = "tm.crosshair.ok";
    public static final String TOOL = "tm.crosshair.tool";
    public static final String HAND = "tm.crosshair.hand";
    public static final String LIMIT = "tm.crosshair.limit";
    public static final String CHEST = "tm.crosshair.chest";
    public static final String MOB = "tm.crosshair.mob";

    /** Everything the HUD needs in one snapshot, sent a few times per second. */
    public static class HudSnapshot {
        public int phase;
        public int outcome;
        public int strikesCurrent;
        public int strikesTotal;
        public int combo;
        public int attention;
        public int secondsToNextEvent;
        public int graceSecondsLeft;
        public int endRushSecondsLeft;
        public int worldDamage;
        public int worldDamageLimit;
        public String crosshairKey = OK;
        public String permitLabel = "";
        public int permitSecondsLeft;
        public boolean perfectEligible = true;
        public boolean eventRunning;
        public String taskKey = "";
        public int[] taskArgs = new int[0];
    }
}
