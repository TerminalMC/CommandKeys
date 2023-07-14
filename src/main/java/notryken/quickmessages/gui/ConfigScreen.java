package notryken.quickmessages.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import notryken.quickmessages.client.QuickMessagesClient;

public class ConfigScreen extends GameOptionsScreen
{
    private ConfigListWidget listWidget;

    public ConfigScreen(Screen parent)
    {
        super(parent, MinecraftClient.getInstance().options,
                Text.literal("Quick Messages"));
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers)
    {
        // Using keyReleased to avoid key press overlap with next screen.
        if (!QuickMessagesClient.getKeyBinding().matchesKey(keyCode, scanCode))
        {
            listWidget.pressedKey(keyCode);
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    protected void init()
    {
        this.listWidget = new ConfigListWidget(client, width, height,
                32, height - 32, 25, parent, Text.literal("Quick Messages"));

        this.addSelectableChild(listWidget);

        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE,
                        (button) -> this.close())
                .size(240, 20)
                .position(this.width / 2 - 120, this.height - 27)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY,
                       float delta)
    {
        this.renderBackground(context);
        this.listWidget.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, 5, 0xffffff);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close()
    {
        QuickMessagesClient.config.purge();
        QuickMessagesClient.saveConfig();
        super.close();
    }
}