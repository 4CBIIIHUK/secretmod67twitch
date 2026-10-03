package dev.twitchmod.attention;

import dev.twitchmod.TwitchMod;
import dev.twitchmod.TwitchModConfig;
import dev.twitchmod.director.EventEffects;
import dev.twitchmod.net.ModNet;
import dev.twitchmod.state.RunState;
import net.minecraft.server.network.ServerPlayerEntity;

/** +1 per clean event, small bonus every five, reset by a real strike. */
public final class ComboManager {
    private ComboManager() {
    }

    public static void onSuccess(RunState s, ServerPlayerEntity p) {
        s.combo++;
        s.bestCombo = Math.max(s.bestCombo, s.combo);
        int every = TwitchModConfig.get().comboBonusEvery;
        if (s.combo % every == 0) {
            ModNet.sendBanner(p, 2, "tm.combo.bonus", 4, s.combo);
            try {
                var inv = p.getInventory();
                inv.insertStack(new net.minecraft.item.ItemStack(net.minecraft.item.Items.GOLD_NUGGET, 8 * every));
                inv.insertStack(new net.minecraft.item.ItemStack(net.minecraft.item.Items.BREAD, 4));
                p.playerScreenHandler.sendContentUpdates();
            } catch (Exception e) {
                TwitchMod.LOGGER.warn("[twitchmod] combo bonus failed: {}", e.toString());
            }
        }
    }

    public static void onStrike(RunState s, ServerPlayerEntity p) {
        if (s.combo > 0) {
            s.combo = 0;
            ModNet.sendBanner(p, 0, "tm.combo.reset", 3, 0);
        }
    }
}
