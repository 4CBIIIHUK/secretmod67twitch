package dev.twitchmod.client.ui;

import dev.twitchmod.client.ClientNet;
import dev.twitchmod.client.ClientState;
import dev.twitchmod.client.hud.TwitchHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * End screen. A real ban offers [Выйти] / [Наблюдать]; a win offers [Продолжить].
 * Nothing here deletes the world file.
 */
public class OutcomeScreen extends Screen {
    private final ClientState.OutcomeScreenData data;

    public OutcomeScreen(ClientState.OutcomeScreenData data) {
        super(Text.translatable(data.titleKey));
        this.data = data;
    }

    @Override
    protected void init() {
        boolean banned = data.outcome == 2;
        if (banned) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("tm.ban.exit"), b -> {
                ClientNet.sendBanChoice(0);
            }).dimensions(this.width / 2 - 155, this.height / 2 + 24, 150, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("tm.ban.observe"), b -> {
                ClientNet.sendBanChoice(1);
                close();
            }).dimensions(this.width / 2 + 5, this.height / 2 + 24, 150, 20).build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.translatable("tm.ban.exit"), b -> close())
                    .dimensions(this.width / 2 - 100, this.height / 2 + 24, 200, 20).build());
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        int color = data.outcome == 2 ? TwitchHud.RED : data.perfect ? TwitchHud.PURPLE : TwitchHud.WHITE;
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 50, color);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.translatable(data.detailKey), this.width / 2,
                this.height / 2 - 32, TwitchHud.WHITE);
        String strikes = Text.translatable("tm.strike.count", data.strikesTotal).getString();
        ctx.drawCenteredTextWithShadow(this.textRenderer, strikes, this.width / 2, this.height / 2 - 12, TwitchHud.ORANGE);
        if (data.perfect) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("tm.outcome.perfect"),
                    this.width / 2, this.height / 2 + 2, TwitchHud.PURPLE);
        }
        if (data.outcome == 2) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("tm.ban.sub"),
                    this.width / 2, this.height / 2 + 6, 0xFFAAAAAA);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
