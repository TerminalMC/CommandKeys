/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.gui.widget.list;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.config.*;
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
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static dev.terminalmc.commandkeys.util.Localization.localized;

/**
 * Contains widgets for editing options of a {@link Profile}, including adding,
 * editing, re-ordering and removing {@link CommandKey} instances.
 */
public class ProfileOptionsList extends KeybindOptionsList {
    private int dragSourceSlot = -1;
    
    public ProfileOptionsList(Minecraft mc, int width, int height, int y,
                              int itemHeight, int entryWidth, int entryHeight,
                              @NotNull Profile profile) {
        super(mc, width, height, y, itemHeight, entryWidth, entryHeight);
        this.profile = profile;

        addEntry(new Entry.ScreenSwitchEntry(entryX, entryWidth, entryHeight, this));

        addEntry(new Entry.HudAndHistoryEntry(entryX, entryWidth, entryHeight, this));

        addEntry(new OptionsList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                localized("option", "profile.keys", "\u2139"),
                Tooltip.create(localized("option", "profile.keys.tooltip")), 500));

        for (CommandKey commandKey : profile.getCmdKeys()) {
            // A CommandKey's message list may be empty, but here we need at
            // least one message, so we add an empty one. Removed in cleanup.
            List<Message> messages = commandKey.getMessages();
            if (messages.isEmpty()) commandKey.addMessage(new Message());
            addEntry(new Entry.CommandKeyEntry(dynEntryX, dynEntryWidth, entryHeight, this, commandKey));
        }
        addEntry(new OptionsList.Entry.ActionButtonEntry(dynEntryX, dynEntryWidth, entryHeight,
                Component.literal("+"), null, -1,
                (button) -> {
                    profile.addCmdKey(new CommandKey(profile));
                    reload();
                }));
    }

    @Override
    public ProfileOptionsList reload(int width, int height, double scrollAmount) {
        ProfileOptionsList newListWidget = new ProfileOptionsList(minecraft, width, height,
                getY(), itemHeight, entryWidth, entryHeight, profile);
        newListWidget.setScrollAmount(scrollAmount);
        return newListWidget;
    }

    // CommandKey widget dragging

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderWidget(graphics, mouseX, mouseY, delta);
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
        OptionsList.Entry hoveredEntry = getEntryAtPosition(mouseX, mouseY);
        int hoveredSlot = children().indexOf(hoveredEntry);
        int offset = cmdKeyListOffset();
        // Check whether the drop location is valid
        if (hoveredEntry instanceof Entry.CommandKeyEntry || hoveredSlot == offset - 1) {
            // Check whether the move operation would actually change anything
            if (hoveredSlot > dragSourceSlot || hoveredSlot < dragSourceSlot - 1) {
                // Account for the list not starting at slot 0
                int sourceIndex = dragSourceSlot - offset;
                int destIndex = hoveredSlot - offset;
                // I can't really explain why
                if (sourceIndex > destIndex) destIndex += 1;
                // Move
                profile.moveCmdKey(sourceIndex, destIndex);
                reload();
            }
        }
        this.dragSourceSlot = -1;
    }

    /**
     * @return The index of the first {@link Entry.CommandKeyEntry} in the
     * {@link OptionsList}.
     */
    private int cmdKeyListOffset() {
        int i = 0;
        for (OptionsList.Entry entry : children()) {
            if (entry instanceof Entry.CommandKeyEntry) return i;
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
                new MainOptionsList(minecraft, screen.width, screen.height, getY(),
                        itemHeight, entryWidth, entryHeight, null)));
    }

    public void openCommandKeyOptionsScreen(CommandKey cmdKey) {
        minecraft.setScreen(new OptionsScreen(minecraft.screen, localized("option", "key"),
                new CommandKeyOptionsList(minecraft, screen.width, screen.height, getY(),
                        itemHeight, entryWidth, entryHeight, cmdKey)));
    }

    private abstract static class Entry extends OptionsList.Entry {

        private static class ScreenSwitchEntry extends Entry {
            ScreenSwitchEntry(int x, int width, int height, ProfileOptionsList listWidget) {
                super();
                int buttonWidth = (width - SPACING) / 2;

                elements.add(Button.builder(localized("option", "profile.switch"),
                                (button) -> listWidget.openMainOptionsScreen())
                        .pos(x, 0)
                        .size(buttonWidth, height)
                        .build());

                elements.add(Button.builder(localized("option", "profile.controls"),
                                (button) -> listWidget.openMinecraftControlsScreen())
                        .pos(x + width - buttonWidth, 0)
                        .size(buttonWidth, height)
                        .build());
            }
        }

        private static class HudAndHistoryEntry extends Entry {
            HudAndHistoryEntry(int x, int width, int height, ProfileOptionsList listWidget) {
                super();
                int buttonWidth = (width - SPACING) / 2;

                CycleButton<Boolean> hudButton = CycleButton.booleanBuilder(
                        CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN),
                                CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED))
                        .withInitialValue(listWidget.profile.showHudMessage)
                        .withTooltip((status) -> Tooltip.create(
                                localized("option", "profile.hud.tooltip")))
                        .create(x, 0, buttonWidth, height,
                                localized("option", "profile.hud"),
                                (button, status) -> listWidget.profile.showHudMessage = status);
                hudButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(hudButton);

                CycleButton<Boolean> historyButton = CycleButton.booleanBuilder(
                        CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN),
                                CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED))
                        .withInitialValue(listWidget.profile.addToHistory)
                        .withTooltip((status) -> Tooltip.create(
                                localized("option", "profile.history.tooltip")))
                        .create(x + width - buttonWidth, 0, buttonWidth, height,
                                localized("option", "profile.history"),
                                (button, status) -> listWidget.profile.addToHistory = status);
                historyButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(historyButton);
            }
        }

        private static class CommandKeyEntry extends Entry {
            CommandKeyEntry(int x, int width, int height, ProfileOptionsList listWidget,
                            CommandKey cmdKey) {
                super();
                List<Message> messages = cmdKey.getMessages();
                boolean editableField = messages.size() == 1;
                int keyButtonWidth = Math.max(110, Math.min(150, width / 5));
                int messageFieldWidth = width - keyButtonWidth
                        - (3 * listWidget.smallButtonWidth + 4 * SPACING);
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
                                    listWidget.dragSourceSlot = listWidget.children().indexOf(this);
                                })
                        .pos(movingX, 0)
                        .size(listWidget.smallButtonWidth, height)
                        .build());
                movingX += listWidget.smallButtonWidth + SPACING;

                // Keybind button
                MutableComponent[] keybindInfo = KeybindUtil.getKeybindInfo(cmdKey);
                elements.add(Button.builder(keybindInfo[1],
                                (button) -> {
                                    listWidget.selectedCmdKey = cmdKey;
                                    button.setMessage(Component.literal("> ")
                                            .append(keybindInfo[0].withStyle(ChatFormatting.WHITE)
                                                    .withStyle(ChatFormatting.UNDERLINE))
                                            .append(" <").withStyle(ChatFormatting.YELLOW));
                                })
                        .tooltip(Tooltip.create(keybindInfo[2]))
                        .pos(movingX, 0)
                        .size(keyButtonWidth, height)
                        .build());
                movingX += keyButtonWidth + SPACING;

                // Field
                EditBox messageField = new EditBox(Minecraft.getInstance().font, movingX, 0,
                        messageFieldWidth, height, Component.empty());
                messageField.setMaxLength(256);
                messageField.setValue(editableField
                        ? messages.getFirst().string
                        : getEditButtonLabel(cmdKey, messageFieldWidth - 10));
                messageField.setResponder(editableField
                        ? (val) -> cmdKey.setMessage(0, val.strip())
                        : (val) -> listWidget.openCommandKeyOptionsScreen(cmdKey));
                elements.add(messageField);
                movingX += messageFieldWidth + SPACING;

                // Edit button
                ImageButton editButton = new ImageButton(movingX, 0,
                        listWidget.smallButtonWidth, height, GEAR_SPRITES,
                        (button) -> {
                            listWidget.openCommandKeyOptionsScreen(cmdKey);
                            listWidget.reload();
                        });
                editButton.setTooltip(Tooltip.create(localized("option", "profile.key.edit")));
                editButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(editButton);
                movingX += listWidget.smallButtonWidth + SPACING;

                if (modeButtonWidth != 0) {
                    // Conflict strategy button
                    CycleButton<QuadState.State> conflictButton = CycleButton.builder(
                                    KeybindUtil::localizeConflict)
                            .displayOnlyValue()
                            .withValues(QuadState.State.values())
                            .withInitialValue(cmdKey.conflictStrategy.state)
                            .withTooltip((status) -> Tooltip.create(KeybindUtil.localizeConflictTooltip(status)))
                            .create(movingX, 0, modeButtonWidth, height, Component.empty(),
                                    (button, status) -> {
                                        cmdKey.conflictStrategy.state = status;
                                        listWidget.reload();
                                    });
                    elements.add(conflictButton);
                    movingX += modeButtonWidth + SPACING;

                    // Send mode button
                    boolean lockToSend = cmdKey.conflictStrategy.state.equals(QuadState.State.THREE);
                    CycleButton<TriState.State> modeButton = CycleButton.<TriState.State>builder(
                                    (status) -> lockToSend
                                            ? localized("option", "key.mode.send")
                                            .withStyle(ChatFormatting.GREEN)
                                            : KeybindUtil.localizeSendMode(status))
                            .displayOnlyValue()
                            .withValues(TriState.State.values())
                            .withInitialValue(cmdKey.sendStrategy.state)
                            .withTooltip((status) -> Tooltip.create(lockToSend
                                    ? localized("option", "key.mode.forced.tooltip",
                                    KeybindUtil.localizeSendMode(TriState.State.ONE),
                                    KeybindUtil.localizeConflict(QuadState.State.THREE))
                                    : KeybindUtil.localizeSendModeTooltip(status)))
                            .create(movingX, 0, modeButtonWidth, height, Component.empty(),
                                    (button, status) -> {
                                        cmdKey.sendStrategy.state = status;
                                        listWidget.reload();
                                    });
                    if (lockToSend) modeButton.active = false;
                    elements.add(modeButton);
                }

                // Delete button
                elements.add(Button.builder(Component.literal("\u274C")
                                        .withStyle(ChatFormatting.RED),
                                (button) -> {
                                    listWidget.profile.removeCmdKey(cmdKey);
                                    listWidget.reload();
                                })
                        .pos(x + width - listWidget.smallButtonWidth, 0)
                        .size(listWidget.smallButtonWidth, height)
                        .build());
            }

            private String getEditButtonLabel(CommandKey cmdKey, int maxWidth) {
                Font font = Minecraft.getInstance().font;
                List<String> strings = new ArrayList<>();
                for (Message msg : cmdKey.getMessages()) strings.add(msg.string);
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
