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

package dev.terminalmc.commandkeys.gui.widget.list;

import dev.terminalmc.commandkeys.config.*;
import dev.terminalmc.commandkeys.util.KeybindUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.terminalmc.commandkeys.config.Macro.ConflictStrategy.*;
import static dev.terminalmc.commandkeys.config.Macro.SendMode.*;
import static dev.terminalmc.commandkeys.config.Profile.Control.DEFER;
import static dev.terminalmc.commandkeys.util.Localization.localized;

/**
 * Displays widgets for all options of a {@link Macro}, and a list of entries
 * for its {@link Message} instances.
 */
public class MacroOptionList extends MacroBindList {
    private final Macro macro;
    private OptionList.Entry.ActionButton addMessageEntry;

    public MacroOptionList(Minecraft mc, int width, int height, int y, int entryWidth,
                           int entryHeight, int entrySpacing, Profile profile, Macro macro) {
        super(mc, width, height, y, entryWidth, entryHeight, entrySpacing, profile,
                new HashMap<>(Map.of(Entry.MessageOptions.class, macro::moveMessage)));
        this.macro = macro;
        
        addMessageEntry = new OptionList.Entry.ActionButton(
                entryX, entryWidth, entryHeight, Component.literal("+"), null, -1,
                (button) -> {
                    macro.addMessage(new Message());
                    init();
                    ensureVisible(addMessageEntry);
                });
    }
    
    @Override
    protected void addEntries() {
        addEntry(new Entry.DualKeybind(dynEntryX, dynEntryWidth, entryHeight, this, profile, macro));

        if (
                profile.getShowHudMessage().equals(DEFER)
                || profile.getAddToHistory().equals(DEFER)
                || profile.getResumeRepeating().equals(DEFER)
                || profile.getUseRatelimit().equals(DEFER)
        ) {
            addEntry(new Entry.MacroControls(dynEntryX, dynEntryWidth, entryHeight, profile, macro));
        }

        addEntry(new Entry.MacroMode(dynEntryX, dynEntryWidth, entryHeight, this, profile, macro));

        addEntry(new OptionList.Entry.Text(dynEntryX, dynEntryWidth, entryHeight,
                localized("option", "key.messages"), null, -1));

        refreshMessageSubList();
        addMessageEntry.setBounds(dynEntryX, dynEntryWidth, entryHeight);
        addEntry(addMessageEntry);
    }

    protected void refreshMessageSubList() {
        children().removeIf((entry) -> entry instanceof Entry.MessageOptions);
        // Get list start index
        int start = children().indexOf(addMessageEntry);
        if (start == -1) {
            start = children().size();
        } else {
            start--;
        }
        // Add in reverse order
        List<Message> messages = macro.getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            Entry msgEntry = new Entry.MessageOptions(dynWideEntryX, dynWideEntryWidth, entryHeight,
                    this, macro, message, i);
            children().add(start, new OptionList.Entry.Space(msgEntry));
            children().add(start, msgEntry);
        }
        clampScrollAmount();
    }

    // Custom entries

    private abstract static class Entry extends OptionList.Entry {

        private static class DualKeybind extends Entry {
            DualKeybind(int x, int width, int height, MacroOptionList list,
                        Profile profile, Macro macro) {
                super();
                int buttonWidth = (width - SPACE) / 2;

                KeybindUtil.KeybindInfo info =
                        new KeybindUtil.KeybindInfo(profile, macro, macro.getKeybind());
                elements.add(Button.builder(info.conflictLabel,
                                (button) -> {
                                    list.setSelected(macro, macro.getKeybind());
                                    button.setMessage(Component.literal("> ")
                                            .append(info.label.withStyle(ChatFormatting.WHITE)
                                                    .withStyle(ChatFormatting.UNDERLINE))
                                            .append(" <").withStyle(ChatFormatting.YELLOW));
                                })
                        .tooltip(Tooltip.create(info.tooltip))
                        .pos(x, 0)
                        .size(buttonWidth, height)
                        .build());

                KeybindUtil.KeybindInfo altInfo =
                        new KeybindUtil.KeybindInfo(profile, macro, macro.getAltKeybind());
                elements.add(Button.builder(altInfo.conflictLabel,
                                (button) -> {
                                    list.setSelected(macro, macro.getAltKeybind());
                                    button.setMessage(Component.literal("> ")
                                            .append(altInfo.label.withStyle(ChatFormatting.WHITE)
                                                    .withStyle(ChatFormatting.UNDERLINE))
                                            .append(" <").withStyle(ChatFormatting.YELLOW));
                                })
                        .tooltip(Tooltip.create(altInfo.tooltip.getString().isBlank()
                                ? localized("option", "key.bind.alt.tooltip") : altInfo.tooltip))
                        .pos(x + width - buttonWidth, 0)
                        .size(buttonWidth, height)
                        .build());
            }
        }
        
        private static class MacroControls extends Entry {
            MacroControls(int x, int width, int height, Profile profile, Macro macro) {
                super();
                int buttonWidth = (width - SPACE_SMALL * 3) / 4;
                int movingX = x;
                
                boolean hudActive = profile.getShowHudMessage().equals(DEFER);
                CycleButton<Boolean> hudButton = CycleButton.booleanBuilder(
                                CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN),
                                CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED))
                        .withInitialValue(hudActive
                                ? macro.getShowHudMessage() : macro.getShowHudMessageStatus())
                        .withTooltip((status) -> Tooltip.create(
                                localized("option", "macro.hud.tooltip")))
                        .create(movingX, 0, buttonWidth, height,
                                localized("option", "macro.hud"),
                                (button, status) -> profile.setShowHudMessage(macro, status));
                hudButton.setTooltipDelay(Duration.ofMillis(500));
                hudButton.active = hudActive;
                elements.add(hudButton);
                movingX += buttonWidth + SPACE_SMALL;

                boolean historyActive = profile.getAddToHistory().equals(DEFER);
                CycleButton<Boolean> historyButton = CycleButton.booleanBuilder(
                                CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN),
                                CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED))
                        .withInitialValue(historyActive
                                ? macro.getAddToHistory() : macro.getAddToHistoryStatus())
                        .withTooltip((status) -> Tooltip.create(
                                localized("option", "macro.history.tooltip")))
                        .create(movingX, 0, buttonWidth, height,
                                localized("option", "macro.history"),
                                (button, status) -> profile.setAddToHistory(macro, status));
                historyButton.setTooltipDelay(Duration.ofMillis(500));
                historyButton.active = historyActive;
                elements.add(historyButton);
                movingX = x + width - buttonWidth * 2 - SPACE_SMALL;

                boolean resumeActive = profile.getResumeRepeating().equals(DEFER);
                CycleButton<Boolean> resumeButton = CycleButton.booleanBuilder(
                                CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN),
                                CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED))
                        .withInitialValue(resumeActive
                                ? macro.getResumeRepeating() : macro.getResumeRepeatingStatus())
                        .withTooltip((status) -> Tooltip.create(
                                localized("option", "macro.resume.tooltip")))
                        .create(movingX, 0, buttonWidth, height,
                                localized("option", "macro.resume"),
                                (button, status) -> profile.setResumeRepeating(macro, status));
                resumeButton.setTooltipDelay(Duration.ofMillis(500));
                resumeButton.active = resumeActive;
                elements.add(resumeButton);
                movingX += buttonWidth + SPACE_SMALL;

                boolean ratelimitActive = profile.getUseRatelimit().equals(DEFER);
                CycleButton<Boolean> ratelimitButton = CycleButton.booleanBuilder(
                                CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN),
                                CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED))
                        .withInitialValue(ratelimitActive
                                ? macro.getUseRatelimit() : macro.getUseRatelimitStatus())
                        .withTooltip((status) -> Tooltip.create(
                                localized("option", "macro.ratelimit.tooltip")))
                        .create(movingX, 0, buttonWidth, height,
                                localized("option", "macro.ratelimit"),
                                (button, status) -> profile.setUseRatelimit(macro, status));
                ratelimitButton.setTooltipDelay(Duration.ofMillis(500));
                ratelimitButton.active = ratelimitActive;
                elements.add(ratelimitButton);
            }
        }

        private static class MacroMode extends Entry {
            private EditBox delayField;

            MacroMode(int x, int width, int height, MacroOptionList list,
                      Profile profile, Macro macro) {
                super();
                Font font = Minecraft.getInstance().font;
                int buttonWidth = (width - SPACE) / 2;
                int minDelayFieldWidth = font.width("0_") + 8;
                int stopButtonWidth = font.width("Stop") + 8;
                int modeButtonWidth = switch(macro.getMode()) {
                    case SEND -> buttonWidth - minDelayFieldWidth;
                    case TYPE, RANDOM -> buttonWidth;
                    case CYCLE -> buttonWidth - list.smallWidgetWidth;
                    case REPEAT -> buttonWidth -
                            (macro.hasRepeating() ? stopButtonWidth : minDelayFieldWidth);
                };

                // Conflict strategy button
                CycleButton<Macro.ConflictStrategy> conflictButton = CycleButton.builder(
                        KeybindUtil::localizeStrategy)
                        .withValues(Macro.ConflictStrategy.values())
                        .withInitialValue(macro.getStrategy())
                        .withTooltip((status) -> Tooltip.create(
                                KeybindUtil.localizeStrategyTooltip(status)))
                        .create(x, 0, buttonWidth, height,
                                localized("option", "key.conflict"),
                                (button, status) -> {
                                    profile.setConflictStrategy(macro, status);
                                    list.init();
                                });
                elements.add(conflictButton);

                // Send mode button
                CycleButton<Macro.SendMode> modeButton = CycleButton.builder(
                        KeybindUtil::localizeMode)
                        .withValues(Macro.SendMode.values())
                        .withInitialValue(macro.getMode())
                        .withTooltip((status) -> Tooltip.create(
                                KeybindUtil.localizeModeTooltip(status)))
                        .create(x + width - buttonWidth, 0, modeButtonWidth, height,
                                localized("option", "key.mode"),
                                (button, status) -> {
                                    profile.setSendMode(macro, status);
                                    list.init();
                                });
                elements.add(modeButton);

                if (
                        macro.getMode().equals(SEND)
                        || (
                                macro.getMode().equals(REPEAT)
                                && !macro.hasRepeating()
                        )
                ) {
                    // Delay field
                    delayField = new EditBox(font, x + width - minDelayFieldWidth, 0,
                            minDelayFieldWidth, height, Component.empty());
                    delayField.setMaxLength(8);
                    delayField.setResponder((val) -> {
                        // Resize
                        int newWidth = Math.max(minDelayFieldWidth,
                                font.width(val) + font.width("_") + 8);
                        int deltaWidth = delayField.getWidth() - newWidth;
                        modeButton.setWidth(modeButton.getWidth() + deltaWidth);
                        delayField.setX(delayField.getX() + deltaWidth);
                        delayField.setWidth(delayField.getWidth() - deltaWidth);
                        // Actual responder
                        try {
                            int space = Integer.parseInt(val.strip());
                            if (space < 0) throw new NumberFormatException();
                            int oldSpace = macro.spaceTicks;
                            macro.spaceTicks = space;
                            // Show/hide per-message delay fields
                            if (macro.getMode() == SEND
                                    && ((space == 0 && oldSpace != 0) || (space != 0 && oldSpace == 0))) {
                                list.refreshMessageSubList();
                            } else {
                                delayField.setTextColor(16777215);
                            }
                        } catch (NumberFormatException ignored) {
                            delayField.setTextColor(16711680);
                        }
                    });
                    delayField.setValue(String.valueOf(macro.spaceTicks));
                    // Workaround to prevent the value sliding off to the left
                    delayField.setCursorPosition(0);
                    delayField.setHighlightPos(0);
                    delayField.setTooltip(Tooltip.create(
                            localized("option", "key.delay.tooltip"
                                    + (macro.getMode().equals(REPEAT) ? ".repeat" : ""))));

                    elements.add(delayField);
                }
                else if (macro.getMode().equals(REPEAT)) {
                    // Has repeating messages, provide stop button
                    elements.add(Button.builder(localized("option", "key.repeat.stop"),
                            (button) -> {
                                macro.stopRepeating();
                                list.init();
                            })
                            .tooltip(Tooltip.create(
                                    localized("option", "key.repeat.stop.tooltip")))
                            .pos(x + width - stopButtonWidth, 0)
                            .size(stopButtonWidth, height)
                            .build());
                }
                else if (macro.getMode().equals(CYCLE)) {
                    // Cycle index button
                    List<Integer> values = new ArrayList<>();
                    for (int i = 0; i < macro.getMessages().size(); i++) values.add(i);
                    if (values.isEmpty()) values.add(0);
                    if (macro.cycleIndex > values.getLast()) macro.cycleIndex = 0;
                    elements.add(CycleButton.<Integer>builder(
                                    (status) -> Component.literal(status.toString()))
                            .withValues(values)
                            .withInitialValue(macro.cycleIndex)
                            .displayOnlyValue()
                            .withTooltip((status) -> Tooltip.create(
                                    localized("option", "key.cycle.index.tooltip")))
                            .create(x + width - list.smallWidgetWidth, 0,
                                    list.smallWidgetWidth, height, Component.empty(),
                                    (button, status) -> macro.cycleIndex = status));
                }
            }
        }

        private static class MessageOptions extends Entry {
            MessageOptions(int x, int width, int height, MacroOptionList list, Macro macro,
                           Message msg, int index) {
                super();
                Font font = Minecraft.getInstance().font;
                boolean showDelayField = (
                        macro.getStrategy() == AVOID
                        || (macro.getMode() == SEND && macro.spaceTicks == 0)
                        || macro.getMode() == REPEAT
                        || macro.getMode() == RANDOM
                );
                int minDelayFieldWidth = font.width("0__") + 8;
                int msgFieldWidth = width - (showDelayField ? minDelayFieldWidth + SPACE : 0);

                // Drag reorder button
                elements.add(Button.builder(Component.literal("\u2191\u2193"),
                                (button) -> {
                                    this.setDragging(true);
                                    list.startDragging(this, null, false);
                                })
                        .pos(x - list.smallWidgetWidth - SPACE, 0)
                        .size(list.smallWidgetWidth, height)
                        .build());

                // Message field
                MultiLineEditBox messageField = new MultiLineEditBox(font,
                        x, 0, msgFieldWidth, height * 2,
                        Component.empty(), Component.empty());
                messageField.setCharacterLimit(512);
                messageField.setValue(msg.string);
                messageField.setValueListener((val) -> msg.string = val.stripLeading());
                elements.add(messageField);

                // Delay field
                if (showDelayField) {
                    EditBox delayField = new EditBox(font,
                            x + width - minDelayFieldWidth, 0,
                            minDelayFieldWidth, height, Component.empty());
                    delayField.setTooltip(Tooltip.create(
                            localized("option", "key.delay.individual.tooltip"
                                    + (index == 0 ? ".first" : ".subsequent"))));
                    delayField.setTooltipDelay(Duration.ofMillis(500));
                    delayField.setMaxLength(8);
                    delayField.setResponder((val) -> {
                        // Resize
                        int newWidth = Math.max(minDelayFieldWidth,
                                font.width(val) + font.width("__") + 8);
                        int deltaWidth = delayField.getWidth() - newWidth;
                        messageField.setWidth(messageField.getWidth() + deltaWidth);
                        delayField.setX(delayField.getX() + deltaWidth);
                        delayField.setWidth(delayField.getWidth() - deltaWidth);
                        // Actual responder
                        try {
                            int delay = Integer.parseInt(val.strip());
                            if (delay < 0) throw new NumberFormatException();
                            msg.delayTicks = delay;
                            delayField.setTextColor(16777215);
                        } catch (NumberFormatException ignored) {
                            delayField.setTextColor(16711680);
                        }
                    });
                    delayField.setValue(String.valueOf(msg.delayTicks));
                    // Workaround to prevent the value sliding off to the left
                    delayField.setCursorPosition(0);
                    delayField.setHighlightPos(0);
                    elements.add(delayField);
                }

                // Delete button
                elements.add(Button.builder(Component.literal("\u274C")
                                        .withStyle(ChatFormatting.RED),
                                (button) -> {
                                    macro.removeMessage(index);
                                    list.init();
                                })
                        .pos(x + width + SPACE, 0)
                        .size(list.smallWidgetWidth, height)
                        .build());
            }
        }
    }
}
