/*
 * Copyright 2026 TerminalMC
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

import dev.terminalmc.commandkeys.mixin.accessor.MultiLineEditBoxAccessor;
import dev.terminalmc.commandkeys.mixin.accessor.MultilineTextFieldAccessor;
import dev.terminalmc.commandkeys.mixin.accessor.StringViewAccessor;
import dev.terminalmc.commandkeys.mixin.gui.MultiLineEditBoxMixin;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A custom {@link MultiLineEditBox} which supports double-clicking to select words and
 * triple-clicking to select all (with {@link MultilineTextFieldAccessor}), resizing (with
 * {@link MultiLineEditBoxAccessor}), and content validation with warning text color and tooltip
 * (with {@link MultiLineEditBoxMixin}).
 */
public class MultiLineTextField extends MultiLineEditBox {

    public static final long CLICK_CHAIN_TIME = 250L;
    public static final int TEXT_COLOR_DEFAULT = 0xFFE0E0E0;
    public static final int TEXT_COLOR_ERROR = 0xFFFF5555;

    // Validation
    public final List<TextField.@NotNull Validator> validators = new ArrayList<>();
    public boolean lenient = true;
    private int normalTextColor = TEXT_COLOR_DEFAULT;
    private int currentTextColor = normalTextColor;
    private @Nullable Tooltip normalTooltip;
    private @Nullable Tooltip errorTooltip;

    // Undo-redo history
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;

    // Double and triple-click selection
    private long lastClickTime;
    private int chainedClicks;

    public MultiLineTextField(int x, int y, int width, int height) {
        this(
                Minecraft.getInstance().font,
                x,
                y,
                width,
                height,
                Component.empty(),
                Component.empty(),
                null
        );
    }

    public MultiLineTextField(int x, int y, int width, int height, Component placeholder) {
        this(
                Minecraft.getInstance().font,
                x,
                y,
                width,
                height,
                placeholder,
                Component.empty(),
                null
        );
    }

    public MultiLineTextField(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component placeholder,
            Component message,
            @Nullable TextField.Validator validator
    ) {
        super(
                font,
                x,
                y,
                width,
                height,
                placeholder,
                message,
                TEXT_COLOR_DEFAULT,
                true,
                0xFFD0D0D0,
                true,
                true
        );
        if (validator != null) {
            this.validators.add(validator);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public MultiLineTextField withValidator(@NotNull TextField.Validator validator) {
        this.validators.add(validator);
        return this;
    }

    @Override
    public void setValueListener(@NotNull Consumer<String> responder) {
        super.setValueListener((str) -> {
            updateHistory(str);
            if (validate(str) || lenient) {
                responder.accept(str);
            }
        });
    }

    private boolean validate(String str) {
        for (TextField.Validator v : validators) {
            Optional<Component> error = v.validate(str);
            if (error.isPresent()) {
                errorTooltip = Tooltip.create(error.get());
                super.setTooltip(errorTooltip);
                this.currentTextColor = TEXT_COLOR_ERROR;
                return false;
            }
        }
        errorTooltip = null;
        this.currentTextColor = normalTextColor;
        super.setTooltip(normalTooltip);
        return true;
    }

    @Override
    public void setTooltip(@Nullable Tooltip tooltip) {
        normalTooltip = tooltip;
        if (errorTooltip == null) {
            super.setTooltip(tooltip);
        }
    }

    public int getTextColor() {
        return currentTextColor;
    }

    @SuppressWarnings("unused")
    public void setTextColor(int color) {
        normalTextColor = color;
        if (errorTooltip == null) {
            currentTextColor = color;
        }
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        ((MultilineTextFieldAccessor) ((MultiLineEditBoxAccessor) this).commandkeys$getTextField()).commandkeys$setWidth(
                width);
    }

    // Chained clicks

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            // Double-click to select all
            long time = Util.getMillis();
            if (lastClickTime + CLICK_CHAIN_TIME > time) {
                MultilineTextField field =
                        ((MultiLineEditBoxAccessor) this).commandkeys$getTextField();
                MultilineTextFieldAccessor fieldAcc = (MultilineTextFieldAccessor) field;
                switch (++chainedClicks) {
                    case 1 -> {
                        // double-click: select word
                        field.seekCursor(
                                Whence.ABSOLUTE,
                                ((StringViewAccessor) (Object) field.getNextWord()).commandkeys$getBeginIndex()
                        );
                        int pos = fieldAcc.commandkeys$getCursor();
                        //noinspection DataFlowIssue
                        field.seekCursor(
                                Whence.ABSOLUTE,
                                ((StringViewAccessor) (Object) field.getPreviousWord()).commandkeys$getBeginIndex()
                        );
                        fieldAcc.commandkeys$setSelectCursor(pos);
                    }
                    case 2, 3 -> {
                        // triple-click: select all
                        // duplicated for quadruple to inhibit overshoot
                        fieldAcc.commandkeys$setCursor(this.getValue().length());
                        fieldAcc.commandkeys$setSelectCursor(0);
                    }
                    case 4 -> {
                        // quintuple-click: reset chain and deselect all
                        chainedClicks = 0;
                        fieldAcc.commandkeys$setSelectCursor(fieldAcc.commandkeys$getCursor());
                    }
                }
            } else {
                chainedClicks = 0;
            }
            lastClickTime = time;
            return true;
        }
        return false;
    }

    // Undo-redo history

    private void updateHistory(String str) {
        if (historyIndex == -1 || !history.get(historyIndex).equals(str)) {
            if (historyIndex < history.size() - 1) {
                // Remove old history before writing new
                for (int i = history.size() - 1; i > historyIndex; i--) {
                    history.removeLast();
                }
            }
            history.add(str);
            historyIndex++;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!super.keyPressed(keyCode, scanCode, modifiers)) {
            if (TextField.isUndo(keyCode)) {
                undo();
                return true;
            } else if (TextField.isRedo(keyCode)) {
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
}
