package notryken.quickmessages.gui.screen;

import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import notryken.quickmessages.QuickMessages;
import notryken.quickmessages.gui.component.listwidget.ConfigListWidget;
import org.jetbrains.annotations.NotNull;

public abstract class ConfigScreen extends OptionsSubScreen {
    protected ConfigListWidget listWidget;

    public ConfigScreen(Screen parent, Options options, Component title, ConfigListWidget listWidget) {
        super(parent, options, title);
        this.listWidget = listWidget;
    }

    @Override
    protected void init() {
        listWidget = listWidget.resize(this.width, this.height, 32, this.height - 32);
        addWidget(listWidget);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> onClose())
                .size(240, 20)
                .pos(this.width / 2 - 120, this.height - 27)
                .build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listWidget.keyPressed(keyCode, scanCode)) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // Using keyReleased to avoid key press overlap with next screen.
        if (listWidget.keyReleased(keyCode, scanCode)) {
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        listWidget.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, 5, 0xffffff);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderDirtBackground(context);
    }

    @Override
    public void onClose() {
        if (!(lastScreen instanceof ConfigScreen)) {
            QuickMessages.config().purge();
            QuickMessages.config().writeChanges();
        }
        super.onClose();
    }
}
