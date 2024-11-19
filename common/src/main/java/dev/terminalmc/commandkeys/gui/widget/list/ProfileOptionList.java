/*
 * Copyright 2024 TerminalMC
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

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.Macro;
import dev.terminalmc.commandkeys.config.Message;
import dev.terminalmc.commandkeys.config.Profile;
import dev.terminalmc.commandkeys.gui.screen.OptionsScreen;
import dev.terminalmc.commandkeys.util.KeybindUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static dev.terminalmc.commandkeys.util.Localization.localized;

/**
 * Contains widgets for editing options of a {@link Profile}, including adding,
 * editing, re-ordering and removing {@link Macro} instances.
 */
public class ProfileOptionList extends MacroBindList {
    private int dragSourceSlot = -1;
    
    public ProfileOptionList(Minecraft mc, int width, int height, int top, int bottom,
                             int itemHeight, int entryWidth, int entryHeight,
                             @NotNull Profile profile) {
        super(mc, width, height, top, bottom, itemHeight, entryWidth, entryHeight, profile);

        addEntry(new Entry.ScreenSwitchEntry(entryX, entryWidth, entryHeight, this));

        addEntry(new Entry.HudAndHistoryEntry(entryX, entryWidth, entryHeight, this));

        addEntry(new OptionList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                localized("option", "profile.keys", "\u2139"),
                Tooltip.create(localized("option", "profile.keys.tooltip")), 500));

        for (Macro macro : profile.getMacros()) {
            // A CommandKey's message list may be empty, but here we need at
            // least one message, so we add an empty one. Removed in cleanup.
            List<Message> messages = macro.getMessages();
            if (messages.isEmpty()) macro.addMessage(new Message());
            addEntry(new Entry.MacroEntry(dynEntryX, dynEntryWidth, entryHeight, this, profile, macro));
        }
        addEntry(new OptionList.Entry.ActionButtonEntry(dynEntryX, dynEntryWidth, entryHeight,
                Component.literal("+"), null, -1,
                (button) -> {
                    profile.addMacro(new Macro());
                    reload();
                }));
    }

    @Override
    public ProfileOptionList reload(int width, int height, int top, int bottom, double scrollAmount) {
        ProfileOptionList newListWidget = new ProfileOptionList(minecraft, width, height,
                top, bottom, itemHeight, entryWidth, entryHeight, profile);
        newListWidget.setScrollAmount(scrollAmount);
        return newListWidget;
    }

    // CommandKey widget dragging

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        if (dragSourceSlot != -1) {
            super.renderItem(graphics, mouseX, mouseY, delta, dragSourceSlot,
                    mouseX, mouseY, entryWidth, entryHeight);
        }
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (dragSourceSlot != -1 && button == InputConstants.MOUSE_BUTTON_LEFT) {
            dropDragged(x, y);
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    /**
     * A dragged entry, when dropped, will be placed below the hovered entry.
     * Therefore, the move operation will only be executed if the hovered entry
     * is below the dragged entry, or more than one slot above.
     */
    private void dropDragged(double mouseX, double mouseY) {
        OptionList.Entry hoveredEntry = getEntryAtPosition(mouseX, mouseY);
        int hoveredSlot = children().indexOf(hoveredEntry);
        int offset = macroListOffset();
        // Check whether the drop location is valid
        if (hoveredEntry instanceof Entry.MacroEntry || hoveredSlot == offset - 1) {
            // Check whether the move operation would actually change anything
            if (hoveredSlot > dragSourceSlot || hoveredSlot < dragSourceSlot - 1) {
                // Account for the list not starting at slot 0
                int sourceIndex = dragSourceSlot - offset;
                int destIndex = hoveredSlot - offset;
                // I can't really explain why
                if (sourceIndex > destIndex) destIndex += 1;
                // Move
                profile.moveMacro(sourceIndex, destIndex);
                reload();
            }
        }
        this.dragSourceSlot = -1;
    }

    /**
     * @return The index of the first {@link Entry.MacroEntry} in the
     * {@link OptionList}.
     */
    private int macroListOffset() {
        int i = 0;
        for (OptionList.Entry entry : children()) {
            if (entry instanceof Entry.MacroEntry) return i;
            i++;
        }
        throw new IllegalStateException("CommandKey list not found");
    }

    public void openMainOptionsScreen() {
        Screen lastScreen = screen.getLastScreen();
        if (lastScreen instanceof OptionsScreen lastOptionsScreen) {
            lastScreen = lastOptionsScreen.getLastScreen();
        }
        minecraft.setScreen(new OptionsScreen(lastScreen,
                localized("option", "main"),
                new MainOptionList(minecraft, screen.width, screen.height, screen.listTop, 
                        screen.listBottom.get(), itemHeight, entryWidth, entryHeight, null)));
    }

    public void openCommandKeyOptionsScreen(Macro macro) {
        minecraft.setScreen(new OptionsScreen(minecraft.screen, localized("option", "key"),
                new MacroOptionList(minecraft, screen.width, screen.height, screen.listTop, 
                        screen.listBottom.get(), itemHeight, entryWidth, entryHeight, profile, macro)));
    }

    private abstract static class Entry extends OptionList.Entry {

        private static class ScreenSwitchEntry extends Entry {
            ScreenSwitchEntry(int x, int width, int height, ProfileOptionList list) {
                super();
                int buttonWidth = (width - SPACING) / 2;

                elements.add(Button.builder(localized("option", "profile.switch"),
                                (button) -> list.openMainOptionsScreen())
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

        private static class HudAndHistoryEntry extends Entry {
            HudAndHistoryEntry(int x, int width, int height, ProfileOptionList list) {
                super();
                int buttonWidth = (width - SPACING) / 2;

                CycleButton<Profile.Control> hudButton = CycleButton.builder(this::getLabel)
                        .withValues(Profile.Control.values())
                        .withInitialValue(list.profile.getShowHudMessage())
                        .withTooltip((status) -> Tooltip.create(
                                localized("option", "profile.hud.tooltip")))
                        .create(x, 0, buttonWidth, height,
                                localized("option", "macro.hud"),
                                (button, status) -> list.profile.setShowHudMessage(status));
                hudButton.setTooltipDelay(500);
                elements.add(hudButton);

                CycleButton<Profile.Control> historyButton = CycleButton.builder(this::getLabel)
                        .withValues(Profile.Control.values())
                        .withInitialValue(list.profile.getAddToHistory())
                        .withTooltip((status) -> Tooltip.create(
                                localized("option", "profile.history.tooltip")))
                        .create(x + width - buttonWidth, 0, buttonWidth, height,
                                localized("option", "macro.history"),
                                (button, status) -> list.profile.setAddToHistory(status));
                historyButton.setTooltipDelay(500);
                elements.add(historyButton);
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

        private static class MacroEntry extends Entry {
            MacroEntry(int x, int width, int height, ProfileOptionList list, 
                       Profile profile, Macro macro) {
                super();
                List<Message> messages = macro.getMessages();
                boolean editableField = messages.size() == 1;
                int keyButtonWidth = editableField
                        ? Mth.clamp(width / 5, 90, 150)
                        : Mth.clamp(width / 3, 90, 150);
                int messageFieldWidth = width - keyButtonWidth
                        - (4 * list.smallButtonWidth + 5 * SPACING);
                int modeButtonWidth = 0;
                if (messageFieldWidth > 300) {
                    modeButtonWidth = 40;
                    messageFieldWidth -= (modeButtonWidth + SPACING) * 2;
                }
                int movingX = x;

                // Drag reorder button
                elements.add(Button.builder(Component.literal("\u2191\u2193"),
                                (button) -> {
                                    this.setDragging(true);
                                    list.dragSourceSlot = list.children().indexOf(this);
                                })
                        .pos(movingX, 0)
                        .size(list.smallButtonWidth, height)
                        .build());
                movingX += list.smallButtonWidth + SPACING;

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
                movingX += keyButtonWidth + SPACING;

                // Field
                EditBox messageField = new EditBox(Minecraft.getInstance().font, movingX, 0,
                        messageFieldWidth, height, Component.empty());
                messageField.setMaxLength(256);
                messageField.setValue(editableField
                        ? messages.get(0).string
                        : getEditButtonLabel(macro, messageFieldWidth - 10));
                messageField.setResponder(editableField
                        ? (val) -> macro.setMessage(0, val.stripLeading())
                        : (val) -> list.openCommandKeyOptionsScreen(macro));
                elements.add(messageField);
                movingX += messageFieldWidth + SPACING;

                // Send button
                Button sendButton = new ImageButton(movingX, 0, list.smallButtonWidth, height,
                        0, 0, 20, OptionList.Entry.SEND_ICON, 32, 64,
                        (button) -> {
                            list.screen.onClose();
                            Minecraft.getInstance().setScreen(null);
                            macro.trigger(null);
                        });
                sendButton.setTooltip(Tooltip.create(
                        localized("option", "profile.send.tooltip")));
                sendButton.setTooltipDelay(500);
                sendButton.active = CommandKeys.inGame();
                elements.add(sendButton);
                movingX += list.smallButtonWidth + SPACING;

                // Edit button
                ImageButton editButton = new ImageButton(movingX, 0, list.smallButtonWidth, height,
                        0, 0, 20, OptionList.Entry.OPTIONS_ICON, 32, 64,
                        (button) -> {
                            list.openCommandKeyOptionsScreen(macro);
                            list.reload();
                        });
                editButton.setTooltip(Tooltip.create(localized("option", "profile.key.edit")));
                editButton.setTooltipDelay(500);
                elements.add(editButton);
                movingX += list.smallButtonWidth + SPACING;

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
                                        list.reload();
                                    });
                    elements.add(conflictButton);
                    movingX += modeButtonWidth + SPACING;

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
                                        list.reload();
                                    });
                    elements.add(modeButton);
                }

                // Delete button
                elements.add(Button.builder(Component.literal("\u274C")
                                        .withStyle(ChatFormatting.RED),
                                (button) -> {
                                    list.profile.removeMacro(macro);
                                    list.reload();
                                })
                        .pos(x + width - list.smallButtonWidth, 0)
                        .size(list.smallButtonWidth, height)
                        .build());
            }

            private String getEditButtonLabel(Macro macro, int maxWidth) {
                Font font = Minecraft.getInstance().font;
                List<String> strings = new ArrayList<>();
                for (Message msg : macro.getMessages()) strings.add(msg.string);
                int excess = strings.size() - 1;
                String tag = String.format(" [+%d]", excess);
                String trimTag = String.format("... [+%d]", excess);
                String first = strings.get(0);

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
