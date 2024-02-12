package notryken.commandkeys.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import notryken.commandkeys.CommandKeys;
import notryken.commandkeys.gui.component.listwidget.ConfigListWidget;
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
        addRenderableWidget(listWidget);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> onClose())
                .size(240, 20)
                .pos(this.width / 2 - 120, this.height - 27)
                .build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Only go to super if listWidget won't handle following keyReleased.
        if (!listWidget.keyPressed(InputConstants.getKey(keyCode, scanCode))) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // Using keyReleased to prevent key press overlap with next screen.
        // Only go to super if listWidget didn't handle it.
        if (listWidget.handleKey(InputConstants.getKey(keyCode, scanCode))) {
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int delta) {
        if (listWidget.handleKey(InputConstants.Type.MOUSE.getOrCreate(delta))) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, delta);
    }

    @Override
    public void render(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderDirtBackground(context);
        context.drawCenteredString(this.font, this.title, this.width / 2, 5, 0xffffff);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (!(lastScreen instanceof ConfigScreen)) {
            CommandKeys.config().purge();
            CommandKeys.config().writeChanges();
        }
        super.onClose();
    }
}
