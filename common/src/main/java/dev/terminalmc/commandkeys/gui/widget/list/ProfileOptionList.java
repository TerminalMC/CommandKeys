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

import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.*;
import dev.terminalmc.commandkeys.gui.screen.OptionScreen;
import dev.terminalmc.commandkeys.util.KeybindUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.terminalmc.commandkeys.util.Localization.localized;

/**
 * Contains widgets for editing options of a {@link Profile}, including adding,
 * editing, re-ordering and removing {@link Macro} instances.
 */
public class ProfileOptionList extends MacroBindList {
    private OptionList.Entry.ActionButton addMacroEntry;

    public ProfileOptionList(Minecraft mc, int width, int height, int y, int entryWidth,
                             int entryHeight, int entrySpace, @NotNull Profile profile) {
        super(mc, width, height, y, entryWidth, entryHeight, entrySpace, profile,
                new HashMap<>(Map.of(Entry.MacroOptions.class, profile::moveMacro)));

        addMacroEntry = new OptionList.Entry.ActionButton(
                dynWideEntryX, dynWideEntryWidth, entryHeight, Component.literal("+"), null, -1,
                (button) -> {
                    profile.addMacro(new Macro());
                    init();
                    ensureVisible(addMacroEntry);
                });
    }

    @Override
    protected void addEntries() {
        addEntry(new Entry.ScreenSwitch(dynEntryX, dynEntryWidth, entryHeight, this));

        addEntry(new Entry.ProfileControls(dynEntryX, dynEntryWidth, entryHeight, this));

        addEntry(new OptionList.Entry.Text(dynEntryX, dynEntryWidth, entryHeight,
                localized("option", "profile.keys", "\u2139"),
                Tooltip.create(localized("option", "profile.keys.tooltip")), 500));

        refreshMacroSubList();
        addMacroEntry.setBounds(dynEntryX, dynEntryWidth, entryHeight);
        addEntry(addMacroEntry);
    }

    protected void refreshMacroSubList() {
        children().removeIf((entry) -> entry instanceof Entry.MacroOptions);
        // Get list start index
        int start = children().indexOf(addMacroEntry);
        if (start == -1) {
            start = children().size();
        } else {
            start--;
        }
        // Add in reverse order
        List<Macro> macros = profile.getMacros();
        for (int i = macros.size() - 1; i >= 0; i--) {
            Macro macro = macros.get(i);
            // A macro's message list may be empty, but we need at least one
            // for the UI to work, so we add an empty one. Removed in cleanup.
            List<Message> messages = macro.getMessages();
            if (messages.isEmpty()) macro.addMessage(new Message());
            children().add(start, new Entry.MacroOptions(dynWideEntryX, dynWideEntryWidth,
                    entryHeight, this, profile, macro));
        }
        clampScrollAmount();
    }

    // Sub-screen opening

    public void openMainOptions() {
        Screen lastScreen = screen.getLastScreen();
        if (lastScreen instanceof OptionScreen lastOptionScreen) {
            lastScreen = lastOptionScreen.getLastScreen();
        }
        mc.setScreen(new OptionScreen(lastScreen, localized("option", "main"),
                new MainOptionList(mc, width, height, getY(), entryWidth, entryHeight,
                        entrySpacing, null)));
    }

    public void openMacroOptions(Macro macro) {
        mc.setScreen(new OptionScreen(mc.screen, localized("option", "key"),
                new MacroOptionList(mc, width, height, getY(), entryWidth, entryHeight,
                        entrySpacing, profile, macro)));
    }

    private abstract static class Entry extends OptionList.Entry {

        private static class ScreenSwitch extends Entry {
            ScreenSwitch(int x, int width, int height, ProfileOptionList list) {
                super();
                int buttonWidth = (width - SPACE) / 2;

                elements.add(Button.builder(localized("option", "profile.switch"),
                                (button) -> list.openMainOptions())
                        .pos(x, 0)
                        .size(buttonWidth, height)
                        .build());

                elements.add(Button.builder(localized("option", "profile.controls"),
                                (button) -> list.openMinecraftControlsScreen())
                        .pos(x + width - buttonWidth, 0)
                        .size(buttonWidth, height)
                        .build());
            }
        }

        private static class ProfileControls extends Entry {
            ProfileControls(int x, int width, int height, ProfileOptionList list) {
                super();
                int buttonWidth = (width - SPACE_SMALL * 3) / 4;
                int movingX = x;

                CycleButton<Profile.Control> hudButton = CycleButton.builder(this::getLabel)
                        .withValues(Profile.Control.values())
                        .withInitialValue(list.profile.getShowHudMessage())
                        .withTooltip((status) -> Tooltip.create(
                                localized("option", "macro.hud.tooltip").append("\n")
                                        .append(localized("option", "profile.defer.tooltip"))))
                        .create(movingX, 0, buttonWidth, height,
                                localized("option", "macro.hud"),
                                (button, status) -> list.profile.setShowHudMessage(status));
                hudButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(hudButton);
                movingX += buttonWidth + SPACE_SMALL;

                CycleButton<Profile.Control> historyButton = CycleButton.builder(this::getLabel)
                        .withValues(Profile.Control.values())
                        .withInitialValue(list.profile.getAddToHistory())
                        .withTooltip((status) -> Tooltip.create(
                                localized("option", "macro.history.tooltip").append("\n")
                                        .append(localized("option", "profile.defer.tooltip"))))
                        .create(movingX, 0, buttonWidth, height,
                                localized("option", "macro.history"),
                                (button, status) -> list.profile.setAddToHistory(status));
                historyButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(historyButton);
                movingX = x + width - buttonWidth * 2 - SPACE_SMALL;

                CycleButton<Profile.Control> resumeButton = CycleButton.builder(this::getLabel)
                        .withValues(Profile.Control.values())
                        .withInitialValue(list.profile.getResumeRepeating())
                        .withTooltip((status) -> Tooltip.create(
                                localized("option", "macro.resume.tooltip").append("\n")
                                        .append(localized("option", "profile.defer.tooltip"))))
                        .create(movingX, 0, buttonWidth, height,
                                localized("option", "macro.resume"),
                                (button, status) -> list.profile.setResumeRepeating(status));
                resumeButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(resumeButton);
                movingX += buttonWidth + SPACE_SMALL;

                CycleButton<Profile.Control> ratelimitButton = CycleButton.builder(this::getLabel)
                        .withValues(Profile.Control.values())
                        .withInitialValue(list.profile.getUseRatelimit())
                        .withTooltip((status) -> Tooltip.create(
                                localized("option", "macro.ratelimit.tooltip").append("\n")
                                        .append(localized("option", "profile.defer.tooltip"))))
                        .create(movingX, 0, buttonWidth, height,
                                localized("option", "macro.ratelimit"),
                                (button, status) -> list.profile.setUseRatelimit(status));
                ratelimitButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(ratelimitButton);
            }

            private Component getLabel(Profile.Control control) {
                return switch(control) {
                    case ON -> CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN);
                    case OFF -> CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED);
                    case DEFER -> localized("option", "profile.control.defer")
                            .withStyle(ChatFormatting.GOLD);
                };
            }
        }

        private static class MacroOptions extends Entry {
            MacroOptions(int x, int width, int height, ProfileOptionList list,
                         Profile profile, Macro macro) {
                super();
                List<Message> messages = macro.getMessages();
                boolean editableField = messages.size() == 1;
                int keyButtonWidth = editableField
                        ? Mth.clamp(width / 5, 90, 150)
                        : Mth.clamp(width / 3, 90, 150);
                int messageFieldWidth = width - keyButtonWidth
                        - (list.smallWidgetWidth * 2 + SPACE * 3);
                int modeButtonWidth = 0;
                if (messageFieldWidth > 260) {
                    modeButtonWidth = 40;
                    messageFieldWidth -= (modeButtonWidth + SPACE) * 2;
                }
                int movingX = x;

                // Drag reorder button
                elements.add(Button.builder(Component.literal("\u2191\u2193"),
                                (button) -> {
                                    this.setDragging(true);
                                    list.startDragging(this, null, false);
                                })
                        .pos(x - list.smallWidgetWidth - SPACE, 0)
                        .size(list.smallWidgetWidth, height)
                        .build());

                // Keybind button
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
                        .pos(movingX, 0)
                        .size(keyButtonWidth, height)
                        .build());
                movingX += keyButtonWidth + SPACE;

                // Field
                EditBox messageField = new EditBox(Minecraft.getInstance().font, movingX, 0,
                        messageFieldWidth, height, Component.empty());
                messageField.setMaxLength(512);
                messageField.setValue(editableField
                        ? messages.getFirst().string
                        : getEditButtonLabel(macro, messageFieldWidth - 10));
                messageField.setResponder(editableField
                        ? (val) -> macro.setMessage(0, val.stripLeading())
                        : (val) -> list.openMacroOptions(macro));
                elements.add(messageField);
                movingX += messageFieldWidth + SPACE;

                // Send button
                Button sendButton = new ImageButton(movingX, 0,
                        list.smallWidgetWidth, height, SEND_SPRITES,
                        (button) -> {
                            list.screen.onClose();
                            Minecraft.getInstance().setScreen(null);
                            macro.trigger(null);
                        });
                sendButton.setTooltip(Tooltip.create(
                        localized("option", "profile.send.tooltip")));
                sendButton.setTooltipDelay(Duration.ofMillis(500));
                sendButton.active = CommandKeys.inGame();
                elements.add(sendButton);
                movingX += list.smallWidgetWidth + SPACE;

                // Edit button
                ImageButton editButton = new ImageButton(movingX, 0,
                        list.smallWidgetWidth, height, OPTION_SPRITES,
                        (button) -> {
                            list.openMacroOptions(macro);
                            list.init();
                        });
                editButton.setTooltip(Tooltip.create(localized("option", "profile.key.edit")));
                editButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(editButton);
                movingX += list.smallWidgetWidth + SPACE;

                if (modeButtonWidth != 0) {
                    // Conflict strategy button
                    CycleButton<Macro.ConflictStrategy> conflictButton = CycleButton.builder(
                                    KeybindUtil::localizeStrategy)
                            .displayOnlyValue()
                            .withValues(Macro.ConflictStrategy.values())
                            .withInitialValue(macro.getStrategy())
                            .withTooltip((status) -> Tooltip.create(KeybindUtil.localizeStrategyTooltip(status)))
                            .create(movingX, 0, modeButtonWidth, height, Component.empty(),
                                    (button, status) -> {
                                        profile.setConflictStrategy(macro, status);
                                        list.init();
                                    });
                    elements.add(conflictButton);
                    movingX += modeButtonWidth + SPACE;

                    // Send mode button
                    CycleButton<Macro.SendMode> modeButton = CycleButton.builder(
                                    KeybindUtil::localizeMode)
                            .displayOnlyValue()
                            .withValues(Macro.SendMode.values())
                            .withInitialValue(macro.getMode())
                            .withTooltip((status) -> Tooltip.create(
                                    KeybindUtil.localizeModeTooltip(status)))
                            .create(movingX, 0, modeButtonWidth, height, Component.empty(),
                                    (button, status) -> {
                                        profile.setSendMode(macro, status);
                                        list.init();
                                    });
                    elements.add(modeButton);
                }

                // Delete button
                elements.add(Button.builder(Component.literal("\u274C")
                                        .withStyle(ChatFormatting.RED),
                                (button) -> {
                                    list.profile.removeMacro(macro);
                                    list.init();
                                })
                        .pos(x + width + SPACE, 0)
                        .size(list.smallWidgetWidth, height)
                        .build());
            }

            private String getEditButtonLabel(Macro macro, int maxWidth) {
                Font font = Minecraft.getInstance().font;
                List<String> strings = new ArrayList<>();
                for (Message msg : macro.getMessages()) strings.add(msg.string);
                int excess = strings.size() - 1;
                String tag = String.format(" [+%d]", excess);
                String trimTag = String.format("... [+%d]", excess);
                String first = strings.getFirst();

                if (first.isBlank()) {
                    return trimTag;
                } else {
                    String label = first + tag;
                    int i = first.length();
                    while (font.width(label) > maxWidth) {
                        label = first.substring(0, i--) + trimTag;
                    }
                    return label;
                }
            }
        }
    }
}
