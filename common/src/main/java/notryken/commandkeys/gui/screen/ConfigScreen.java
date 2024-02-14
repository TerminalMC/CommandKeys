package notryken.commandkeys.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import notryken.commandkeys.CommandKeys;
import notryken.commandkeys.gui.component.listwidget.ConfigListWidget;
import notryken.commandkeys.gui.component.listwidget.MonoKeySetListWidget;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * A {@code ConfigScreen} contains one tightly-coupled {@code ConfigListWidget},
 * which is used to display all configuration options required for the screen.
 */
public class ConfigScreen extends OptionsSubScreen {

    protected ConfigListWidget listWidget;

    public final int listTop = 32;
    public final Supplier<Integer> listBottom = () -> height - 32;
    public final int listItemHeight = 25;

    public ConfigScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options,
                Component.translatable("screen.commandkeys.title.default"));
        listWidget = new MonoKeySetListWidget(Minecraft.getInstance(), 0, 0, 0, 0,
                0, -200, 400, 20, 420);
    }

    public ConfigScreen(Screen lastScreen, Component title, ConfigListWidget listWidget) {
        super(lastScreen, Minecraft.getInstance().options, title);
        this.listWidget = listWidget;
    }

    @Override
    protected void init() {
        listWidget = listWidget.resize(width, height, listTop, listBottom.get(), listItemHeight, listWidget.getScrollAmount());
        listWidget.setScreen(this);
        addRenderableWidget(listWidget);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE,
                        (button) -> onClose())
                .pos(width / 2 - 120, height - 27)
                .size(240, 20)
                .build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listWidget.willHandleKey(InputConstants.getKey(keyCode, scanCode))) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // Keys handled by DualKeyListWidget may trigger a screen switch, so we
        // pass on keyReleased instead of keyPressed to avoid key overlap
        // TODO check whether this is necessary, maybe can use screen onClose()
        if (listWidget.handleKey(InputConstants.getKey(keyCode, scanCode))) return true;
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int delta) {
        if (listWidget.handleKey(InputConstants.Type.MOUSE.getOrCreate(delta))) return true;
        return super.mouseClicked(mouseX, mouseY, delta);
    }

    @Override
    public void render(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderDirtBackground(context);
        context.drawCenteredString(font, title, width / 2, 5, 0xffffff);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (!(lastScreen instanceof ConfigScreen)) {
            CommandKeys.config().writeToFile();
        }
        super.onClose();
    }

    public Screen getLastScreen() {
        return lastScreen;
    }

    public void reload() {
        minecraft.setScreen(new ConfigScreen(lastScreen, title, listWidget));
    }
}
