/*
 * Copyright 2025 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.terminalmc.commandkeys.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.Config;
import dev.terminalmc.commandkeys.gui.widget.list.OptionList;
import dev.terminalmc.commandkeys.gui.widget.list.ProfileOptionList;
import dev.terminalmc.commandkeys.gui.widget.list.MainOptionList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import static dev.terminalmc.commandkeys.util.Localization.localized;

/**
 * Contains one tightly-coupled {@link OptionList}, which is used to display
 * all option control widgets.
 */
public class OptionsScreen extends OptionsSubScreen {
    public static final int TOP_MARGIN = 32;
    public static final int BOTTOM_MARGIN = 32;
    public static final int LIST_ENTRY_SPACE = 25;
    public static final int LIST_ENTRY_HEIGHT = 20;
    public static final int BASE_ROW_WIDTH = Window.BASE_WIDTH;
    public static final int BASE_LIST_ENTRY_WIDTH = BASE_ROW_WIDTH - 20;

    protected OptionList optionList;

    public OptionsScreen(Screen lastScreen, boolean inGame) {
        super(lastScreen, Minecraft.getInstance().options,
                inGame ? localized("option", "profile", CommandKeys.profile().getDisplayName())
                        : localized("option", "main"));
        if (inGame) {
            optionList = new ProfileOptionList(Minecraft.getInstance(), 0, 0, TOP_MARGIN,
                    LIST_ENTRY_SPACE, BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT,
                    CommandKeys.profile());
        }
        else {
            optionList = new MainOptionList(Minecraft.getInstance(), 0, 0, TOP_MARGIN,
                    LIST_ENTRY_SPACE, BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, null);
        }
    }

    public OptionsScreen(Screen lastScreen, Component title, OptionList optionList) {
        super(lastScreen, Minecraft.getInstance().options, title);
        this.optionList = optionList;
    }

    @Override
    protected void init() {
        reload();
    }

    @Override
    protected void addOptions() {
        // Not currently used
    }

    @Override
    public void resize(@NotNull Minecraft mc, int width, int height) {
        super.resize(mc, width, height);
        clearWidgets();
        init();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (optionList.keyPressed(InputConstants.getKey(keyCode, scanCode))) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (optionList.keyReleased(InputConstants.getKey(keyCode, scanCode))) return true;
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int delta) {
        if (optionList.mouseClicked(InputConstants.Type.MOUSE.getOrCreate(delta))) return true;
        return super.mouseClicked(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int delta) {
        if (optionList.mouseReleased(InputConstants.Type.MOUSE.getOrCreate(delta))) return true;
        return super.mouseReleased(mouseX, mouseY, delta);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (lastScreen instanceof OptionsScreen screen) {
            screen.reload(width, height);
        } else {
            Config.save();
        }
        super.onClose();
    }

    public Screen getLastScreen() {
        return lastScreen;
    }

    public OptionList reload() {
        return reload(width, height);
    }

    public OptionList reload(int width, int height) {
        clearWidgets();
        optionList = optionList.reload(this, width, height - TOP_MARGIN - BOTTOM_MARGIN,
                optionList.getScrollAmount());
        addRenderableWidget(optionList);

        // Title text
        Font font = Minecraft.getInstance().font;
        addRenderableWidget(new StringWidget(width / 2 - (font.width(title) / 2),
                Math.max(0, TOP_MARGIN / 2 - font.lineHeight / 2),
                font.width(title), font.lineHeight, title, font).alignLeft());

        // Done button
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> onClose())
                .pos(width / 2 - BASE_LIST_ENTRY_WIDTH / 2, Math.min(height - LIST_ENTRY_HEIGHT,
                        height - BOTTOM_MARGIN / 2 - LIST_ENTRY_HEIGHT / 2))
                .size(BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT)
                .build());

        return optionList;
    }
}
