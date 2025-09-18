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

package dev.terminalmc.commandkeys.gui.widget.field;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.mixin.accessor.EditBoxAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static dev.terminalmc.commandkeys.util.Localization.localized;

/**
 * A custom {@link EditBox} which supports click-dragging to select text, double-clicking to select
 * words, triple-clicking to select all, and content validation with warning text color and
 * tooltip.
 */
@SuppressWarnings("UnusedReturnValue")
public class TextField extends EditBox {

    public static final long CLICK_CHAIN_TIME = 250L;
    public static final int TEXT_COLOR_DEFAULT = 0xE0E0E0;
    public static final int TEXT_COLOR_ERROR = 0xFF5555;
    public static final int TEXT_COLOR_HINT = 0x555555;
    public static final int TEXT_COLOR_PREVIEW = 0xAAAAAA;

    private final Font font;

    // Validation
    public final List<@NotNull Validator> validators = new ArrayList<>();
    public boolean lenient = true;
    private int normalTextColor = TEXT_COLOR_DEFAULT;
    private @Nullable Tooltip normalTooltip;
    private @Nullable Tooltip errorTooltip;

    // Undo-redo history
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;

    // Click-drag selection
    private double dragOriginX;
    private int dragOriginPos;

    // Double and triple-click selection
    private long lastClickTime;
    private int chainedClicks;

    public TextField(int x, int y, int width, int height) {
        this(Minecraft.getInstance().font, x, y, width, height, Component.empty(), null);
    }

    public TextField(int x, int y, int width, int height, @Nullable Validator validator) {
        this(Minecraft.getInstance().font, x, y, width, height, Component.empty(), validator);
    }

    public TextField(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component msg,
            @Nullable Validator validator
    ) {
        super(font, x, y, width, height, msg);
        this.font = font;
        if (validator != null) {
            this.validators.add(validator);
        }
    }

    public TextField withValidator(@NotNull Validator validator) {
        this.validators.add(validator);
        return this;
    }

    public TextField posIntValidator() {
        this.validators.add(new Validator.PosInt());
        return this;
    }

    public TextField strict() {
        this.lenient = false;
        return this;
    }

    @SuppressWarnings("unused")
    public TextField lenient() {
        this.lenient = true;
        return this;
    }

    @Override
    public void setResponder(@NotNull Consumer<String> responder) {
        super.setResponder((str) -> {
            updateHistory(str);
            if (validate(str) || lenient) {
                responder.accept(str);
            }
        });
    }

    private boolean validate(String str) {
        for (Validator v : validators) {
            Optional<Component> error = v.validate(str);
            if (error.isPresent()) {
                errorTooltip = Tooltip.create(error.get());
                super.setTooltip(errorTooltip);
                super.setTextColor(TEXT_COLOR_ERROR);
                return false;
            }
        }
        errorTooltip = null;
        super.setTextColor(normalTextColor);
        super.setTooltip(normalTooltip);
        return true;
    }

    @Override
    public void setHint(@NotNull Component hint) {
        super.setHint(hint.copy().withStyle(Style.EMPTY.withColor(TEXT_COLOR_HINT)));
    }

    @Override
    public void setTooltip(@Nullable Tooltip tooltip) {
        normalTooltip = tooltip;
        if (errorTooltip == null) {
            super.setTooltip(tooltip);
        }
    }

    @Override
    public void setTextColor(int color) {
        normalTextColor = color;
        if (errorTooltip == null) {
            super.setTextColor(color);
        }
    }

    // Chained clicks and click-drag

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            long time = Util.getMillis();
            if (lastClickTime + CLICK_CHAIN_TIME > time) {
                switch (++chainedClicks) {
                    case 1 -> {
                        // double-click: select word
                        int pos = getCursorPosition();
                        int start = pos;
                        // If next char is space or previous char is not space,
                        // go backwards to the start of the word.
                        if (pos < 0) {
                            start = 0;
                        } else if (pos >= getValue().length() || getValue().charAt(pos) == ' ' || (
                                pos > 0 && getValue().charAt(pos - 1) != ' ')) {
                            start = getWordPosition(-1);
                        }
                        int end = getWordPosition(1);
                        moveCursorTo(start, false);
                        moveCursorTo(end, true);
                    }
                    case 2, 3 -> {
                        // triple-click: select all
                        // duplicated for quadruple to inhibit overshoot
                        moveCursorToEnd(false);
                        setHighlightPos(0);
                    }
                    case 4 -> {
                        // quintuple-click: reset chain and deselect all
                        chainedClicks = 0;
                        setHighlightPos(getCursorPosition());
                    }
                }
            } else {
                chainedClicks = 0;
            }
            lastClickTime = time;

            // Reset drag origin
            dragOriginX = mouseX;
            dragOriginPos = getCursorPosition();

            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        if (button != 0)
            return false;
        String str = getValue();

        if (mouseX < dragOriginX) { // Dragging left
            String subLeft = str.substring(0, dragOriginPos);
            int offsetChars =
                    font.plainSubstrByWidth(subLeft, Mth.floor(dragOriginX - mouseX), true)
                            .length();
            moveCursorTo(dragOriginPos - offsetChars, true);
        } else { // Dragging right
            String subRight = str.substring(dragOriginPos);
            int offsetChars =
                    font.plainSubstrByWidth(subRight, Mth.floor(mouseX - dragOriginX), false)
                            .length();
            moveCursorTo(dragOriginPos + offsetChars, true);
        }

        return true;
    }

    public void moveCursorTo(int delta, boolean select) {
        setCursorPosition(delta);
        if (!select) {
            setHighlightPos(getCursorPosition());
        }

        onValueChange(((EditBoxAccessor) this).commandkeys$getValue());
    }

    public void moveCursorToEnd(boolean select) {
        this.moveCursorTo(((EditBoxAccessor) this).commandkeys$getValue().length(), select);
    }

    private void onValueChange(String newText) {
        if (((EditBoxAccessor) this).commandkeys$getResponder() != null) {
            ((EditBoxAccessor) this).commandkeys$getResponder().accept(newText);
        }
    }

    // Undo-redo history

    private void updateHistory(String str) {
        if (historyIndex == -1 || !history.get(historyIndex).equals(str)) {
            if (historyIndex < history.size() - 1) {
                // Remove old history before writing new
                if (history.size() > historyIndex + 1) {
                    history.subList(historyIndex + 1, history.size()).clear();
                }
            }
            history.add(str);
            historyIndex++;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!super.keyPressed(keyCode, scanCode, modifiers)) {
            if (isUndo(keyCode)) {
                undo();
                return true;
            } else if (isRedo(keyCode)) {
                redo();
                return true;
            }
            return false;
        }
        return true;
    }

    private void undo() {
        if (historyIndex > 0) {
            setValue(history.get(--historyIndex));
        }
    }

    private void redo() {
        if (historyIndex < history.size() - 1) {
            setValue(history.get(++historyIndex));
        }
    }

    // Validator

    @FunctionalInterface
    public interface Validator {

        Optional<Component> validate(String str);

        // Implementations

        class PosInt implements Validator {

            @Override
            public Optional<Component> validate(String str) {
                try {
                    if (Integer.parseInt(str) < 0)
                        throw new NumberFormatException();
                    return Optional.empty();
                } catch (NumberFormatException ignored) {
                    return Optional.of(localized("ui", "field.error.pos_int").withStyle(
                            ChatFormatting.RED));
                }
            }
        }
    }

    // Utility methods

    public static boolean isUndo(int keyCode) {
        return keyCode == InputConstants.KEY_Z && Screen.hasControlDown() && !Screen.hasShiftDown()
                && !Screen.hasAltDown();
    }

    public static boolean isRedo(int keyCode) {
        return keyCode == InputConstants.KEY_Y && Screen.hasControlDown() && !Screen.hasShiftDown()
                && !Screen.hasAltDown();
    }
}
