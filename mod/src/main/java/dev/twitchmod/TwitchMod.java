package dev.twitchmod;

import dev.twitchmod.command.ModCommands;
import dev.twitchmod.director.EventCatalog;
import dev.twitchmod.cutscene.CutsceneLibrary;
import dev.twitchmod.log.EditorLog;
import dev.twitchmod.net.ModNet;
import dev.twitchmod.rules.ActionGuard;
import dev.twitchmod.run.RunLifecycle;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * "Minecraft, but by Twitch rules" — main entry point.
 *
 * <p>Server side owns the truth: rules, strikes, run state, event director.
 * The client only draws what the server tells it to draw.</p>
 */
public class TwitchMod implements ModInitializer {
    public static final String MOD_ID = "twitchmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        TwitchModConfig.load();
        EditorLog.init();

        // Action rules first: nothing may hand out a strike before the guard exists.
        ActionGuard.register();
        RunLifecycle.register();
        ModNet.registerServer();
        ModCommands.register();

        LOGGER.info("[twitchmod] run format ready: {} events, {} cutscenes, rules armed",
                EventCatalog.size(), CutsceneLibrary.size());
    }
}
