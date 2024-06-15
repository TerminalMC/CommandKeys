/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.Config;
import dev.terminalmc.commandkeys.gui.widget.list.OptionsList;
import dev.terminalmc.commandkeys.gui.widget.list.ProfileEditList;
import dev.terminalmc.commandkeys.gui.widget.list.ProfileSelectList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import static dev.terminalmc.commandkeys.util.Localization.localized;

/**
 * A {@code ConfigScreen} contains one tightly-coupled {@code ConfigListWidget},
 * which is used to display all configuration options required for the screen.
 */
public class OptionsScreen extends OptionsSubScreen {

    protected OptionsList listWidget;

    public final int listTop = 32;
    public final int bottomMargin = 32;
    public final int listItemHeight = 25;

    public OptionsScreen(Screen lastScreen, boolean inGame) {
        super(lastScreen, Minecraft.getInstance().options,
                inGame ? localized("screen", "edit_profile", CommandKeys.profile().getDisplayName())
                        : localized("screen", "select_profile"));
        if (inGame) {
            listWidget = new ProfileEditList(Minecraft.getInstance(), 0, 0, 0,
                    0, -200, 400, 20, 420,
                    CommandKeys.profile(), null);
        }
        else {
            listWidget = new ProfileSelectList(Minecraft.getInstance(), 0, 0, 0,
                    0, -180, 360, 20, 380, null);
        }

    }

    public OptionsScreen(Screen lastScreen, Component title, OptionsList listWidget) {
        super(lastScreen, Minecraft.getInstance().options, title);
        this.listWidget = listWidget;
    }

    @Override
    protected void init() {
        listWidget = listWidget.resize(width, height - listTop - bottomMargin, listTop,
                listItemHeight, listWidget.getScrollAmount());
        listWidget.setScreen(this);
        addRenderableWidget(listWidget);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE,
                        (button) -> onClose())
                .pos(width / 2 - 120, height - 27)
                .size(240, 20)
                .build());
    }

    @Override
    protected void addOptions() {
        // TODO
    }

    @Override
    public void resize(@NotNull Minecraft mc, int width, int height) {
        super.resize(mc, width, height);
        clearWidgets();
        init();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listWidget.keyPressed(InputConstants.getKey(keyCode, scanCode))) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (listWidget.keyReleased(InputConstants.getKey(keyCode, scanCode))) return true;
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int delta) {
        if (listWidget.mouseClicked(InputConstants.Type.MOUSE.getOrCreate(delta))) return true;
        return super.mouseClicked(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int delta) {
        if (listWidget.mouseReleased(InputConstants.Type.MOUSE.getOrCreate(delta))) return true;
        return super.mouseReleased(mouseX, mouseY, delta);
    }

    @Override
    public void render(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(font, title, width / 2, 5, 0xffffff);
    }

    @Override
    public void onClose() {
        if (!(lastScreen instanceof OptionsScreen)) {
            Config.save();
        }
        super.onClose();
    }

    public Screen getLastScreen() {
        return lastScreen;
    }

    public void reload() {
        minecraft.setScreen(new OptionsScreen(lastScreen, title, listWidget));
    }
}
