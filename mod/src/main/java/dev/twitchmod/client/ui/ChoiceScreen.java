package dev.twitchmod.client.ui;

import dev.twitchmod.client.ClientNet;
import dev.twitchmod.client.ClientState;
import dev.twitchmod.client.hud.TwitchHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/** Simulated chat vote / risk-or-safe / appeal offer. Always labelled as a simulation. */
public class ChoiceScreen extends Screen {
    private final ClientState.Choice choice;

    public ChoiceScreen(ClientState.Choice choice) {
        super(Text.translatable(choice.titleKey));
        this.choice = choice;
    }

    @Override
    protected void init() {
        int y = this.height / 2 + 6;
        for (int i = 0; i < choice.optionKeys.size(); i++) {
            final int index = i;
            String label = Text.translatable(choice.optionKeys.get(i)).getString()
                    + (i < choice.percents.size() ? "  " + choice.percents.get(i) + "%" : "");
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> pick(index))
                    .dimensions(this.width / 2 - 155, y + i * 24, 310, 20).build());
        }
    }

    private void pick(int index) {
        ClientNet.sendChoicePick(choice.id, index);
        ClientState.choice = null;
        close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 42,
                TwitchHud.PURPLE);
        if (choice.simulated) {
            String sim = Text.translatable("tm.chat.simulated").getString();
            ctx.drawCenteredTextWithShadow(this.textRenderer, sim, this.width / 2, this.height / 2 - 26, TwitchHud.ORANGE);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        if (Util.getMeasuringTimeMs() > choice.expireAt) {
            ClientState.choice = null;
            close();
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
