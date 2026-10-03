package dev.twitchmod;

import dev.twitchmod.client.ClientNet;
import dev.twitchmod.client.hud.TwitchHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Client entry: HUD, networking mirrors, cutscene player. No rules live here. */
@Environment(EnvType.CLIENT)
public class TwitchModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TwitchModConfig.load();
        ClientNet.register();
        TwitchHud.register();
        TwitchMod.LOGGER.info("[twitchmod] client ready: hud, cutscene camera, subtitles");
    }
}
