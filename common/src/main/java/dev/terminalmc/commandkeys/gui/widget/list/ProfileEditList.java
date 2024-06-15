/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.gui.widget.list;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.CommandKey;
import dev.terminalmc.commandkeys.config.Profile;
import dev.terminalmc.commandkeys.config.QuadState;
import dev.terminalmc.commandkeys.config.TriState;
import dev.terminalmc.commandkeys.gui.screen.OptionsScreen;
import dev.terminalmc.commandkeys.mixin.KeyMappingAccessor;
import dev.terminalmc.commandkeys.util.KeyUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static dev.terminalmc.commandkeys.util.Localization.localized;

/**
 * <p>Contains various widgets for editing options of a {@link Profile},
 * including adding, editing and removing {@link CommandKey} instances.</p>
 */
public class ProfileEditList extends OptionsList {
    Profile profile;
    Set<CommandKey> expandedKeys;
    CommandKey selectedCommandKey;
    InputConstants.Key heldKey;
    InputConstants.Key sendKey;
    
    public ProfileEditList(Minecraft minecraft, int width, int height, int y,
                           int itemHeight, int entryRelX, int entryWidth, int entryHeight,
                           int scrollWidth, @NotNull Profile profile,
                           @Nullable Set<CommandKey> expandedKeys) {
        super(minecraft, width, height, y, itemHeight, entryRelX,
                entryWidth, entryHeight, scrollWidth);
        this.profile = profile;
        this.expandedKeys = (expandedKeys == null) ? new HashSet<>() : expandedKeys;

        addEntry(new Entry.GlobalSettingEntry(entryX, entryWidth, entryHeight, this));

        addEntry(new OptionsList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Command Keys \u2139"),
                Tooltip.create(Component.literal("The messages for each key will be sent if you press the " +
                        "corresponding hotkey while in-game (depending on individual settings).\n" +
                        "You can also send the message(s) for any CommandKey with a single bound key by pressing " +
                        "that key whilst on this screen with nothing selected.")), 500));

        for (CommandKey commandKey : profile.getCmdKeys()) {
            // A CommandKey's message list may be empty, but here we need at
            // least one message, so we add an empty one. Removed in cleanup.
            if (commandKey.messages.isEmpty()) commandKey.messages.add("");

            addEntry(new Entry.CommandKeyOptionsEntry(entryX, entryWidth, entryHeight, this, commandKey));
            if (this.expandedKeys.contains(commandKey)) {
                for (int i = 0; i < commandKey.messages.size(); i++) {
                    addEntry(new Entry.CommandKeyMessageEntry(entryX, entryWidth, entryHeight, this,
                            commandKey, i));
                }
                addEntry(new OptionsList.Entry.ActionButtonEntry(entryX + 25, 0, entryWidth - 50,
                        (int)(entryHeight * 0.7), Component.literal("+"),
                        Tooltip.create(Component.literal("New message")), 500,
                        (button) -> {
                            commandKey.messages.add("");
                            reload();
                        }));
            }
            else {
                addEntry(new Entry.CommandKeyMessageTeaserEntry(entryX, entryWidth, (int)(entryHeight * 0.7), this, commandKey));
            }
        }
        addEntry(new OptionsList.Entry.ActionButtonEntry(entryX, 0, entryWidth, entryHeight,
                Component.literal("+"), null, -1,
                (button) -> {
                    profile.addCmdKey(new CommandKey(profile));
                    reload();
                }));
    }

    @Override
    public OptionsList resize(int width, int height, int y,
                                                                  int itemHeight, double scrollAmount) {
        ProfileEditList newListWidget = new ProfileEditList(
                minecraft, width, height, y, itemHeight, entryRelX,
                entryWidth, entryHeight, scrollWidth, profile, expandedKeys);
        newListWidget.setScrollAmount(scrollAmount);
        return newListWidget;
    }

    @Override
    public boolean keyPressed(InputConstants.Key key) {
        if (selectedCommandKey != null) {
            if (key.getValue() == InputConstants.KEY_ESCAPE) {
                selectedCommandKey.setKey(InputConstants.UNKNOWN);
                selectedCommandKey.setLimitKey(InputConstants.UNKNOWN);
                reload();
            }
            else {
                if (heldKey == null) {
                    heldKey = key;
                }
                else {
                    if (key != heldKey) {
                        selectedCommandKey.setLimitKey(heldKey);
                        selectedCommandKey.setKey(key);
                        reload();
                    }
                    else {
                        return false;
                    }
                }
                return false;
            }
            return true;
        }
        else if (getSelected() == null && !key.equals(((KeyMappingAccessor)CommandKeys.CONFIG_KEY).getKey())) {
            sendKey = key;
        }
        return false;
    }

    @Override
    public boolean keyReleased(InputConstants.Key key) {
        if (selectedCommandKey != null) {
            if (heldKey == key) {
                selectedCommandKey.setKey(key);
                selectedCommandKey.setLimitKey(InputConstants.UNKNOWN);
                reload();
                return true;
            }
        }
        else if (key.equals(sendKey)) {
            if (getSelected() == null) {
                Set<CommandKey> cmdKeys = profile.commandKeyMap.get(sendKey);
                for (CommandKey cmdKey : cmdKeys) {
                    if (cmdKey.conflictStrategy.state.equals(QuadState.State.THREE)) {
                        screen.onClose();
                        minecraft.setScreen(null);
                        for (String msg : cmdKey.messages) {
                            if (!msg.isBlank()) CommandKeys.send(msg,
                                    profile.addToHistory, profile.showHudMessage);
                        }
                        return true;
                    }
                }
            }
            else {
                sendKey = null;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(InputConstants.Key key) {
        return keyPressed(key);
    }

    @Override
    public boolean mouseReleased(InputConstants.Key key) {
        return keyReleased(key);
    }

    public void openProfileSetScreen() {
        Screen lastScreen = screen.getLastScreen();
        if (lastScreen instanceof OptionsScreen lastOptionsScreen) {
            lastScreen = lastOptionsScreen.getLastScreen();
        }
        minecraft.setScreen(new OptionsScreen(lastScreen,
                localized("screen", "select_profile"),
                new ProfileSelectList(minecraft, screen.width, screen.height, getY(),
                        itemHeight, -180, 360, entryHeight, 380, null)));
    }

    public void openMinecraftControlsScreen() {
        minecraft.setScreen(new KeyBindsScreen(screen, Minecraft.getInstance().options));
    }

    private abstract static class Entry extends OptionsList.Entry {

        private static class GlobalSettingEntry extends Entry {
            GlobalSettingEntry(int x, int width, int height, ProfileEditList listWidget) {
                super();
                int spacing = 1;
                int buttonWidth = (width - spacing * 3) / 4;
                CycleButton<Boolean> historyButton = CycleButton.booleanBuilder(
                        Component.literal("Yes").withStyle(ChatFormatting.GREEN), 
                                Component.literal("No").withStyle(ChatFormatting.RED))
                        .withInitialValue(listWidget.profile.addToHistory)
                        .withTooltip((status) -> Tooltip.create(Component.nullToEmpty(
                                "Add sent messages/commands to history.")))
                        .create(x, 0, buttonWidth, height,
                                Component.literal("Chat History"),
                                (button, status) -> listWidget.profile.addToHistory = status);
                historyButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(historyButton);
                CycleButton<Boolean> displayButton = CycleButton.booleanBuilder(
                        Component.literal("Yes").withStyle(ChatFormatting.GREEN),
                                Component.literal("No").withStyle(ChatFormatting.RED))
                        .withInitialValue(listWidget.profile.showHudMessage)
                        .withTooltip((status) -> Tooltip.create(Component.nullToEmpty(
                                "Briefly show the sent message/command as a pop-up above the hotbar.")))
                        .create(x + buttonWidth + spacing, 0, buttonWidth, height,
                                Component.literal("HUD Display"),
                                (button, status) -> listWidget.profile.showHudMessage = status);
                displayButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(displayButton);
                // Switch to right-justified
                elements.add(Button.builder(Component.literal("Change Profile"),
                                (button) -> listWidget.openProfileSetScreen())
                        .pos(x + width - buttonWidth * 2 - spacing, 0)
                        .size(buttonWidth, height)
                        .build());
                elements.add(Button.builder(Component.literal("Minecraft Controls"),
                                (button) -> listWidget.openMinecraftControlsScreen())
                        .pos(x + width - buttonWidth, 0)
                        .size(buttonWidth, height)
                        .build());
            }
        }

        private static class CommandKeyOptionsEntry extends Entry {
            CommandKeyOptionsEntry(int x, int width, int height, ProfileEditList listWidget,
                                   CommandKey commandKey) {
                super();
                int spacing = 5;
                int smallButtonWidth = 20;
                int largeButtonWidth = (width - smallButtonWidth * 2 - spacing * 4) / 3;
                int movingX = x;

                ImageButton collapseButton = new ImageButton(movingX, 0, smallButtonWidth, height,
                        new WidgetSprites(COLLAPSE_ICON, COLLAPSE_DISABLED_ICON,
                                COLLAPSE_HIGHLIGHTED_ICON),
                        (button) -> {
                            listWidget.expandedKeys.remove(commandKey);
                            listWidget.reload();
                        },
                        Component.empty());
                if (listWidget.expandedKeys.contains(commandKey)) {
                    collapseButton.setTooltip(Tooltip.create(Component.literal("Collapse")));
                    collapseButton.setTooltipDelay(Duration.ofMillis(500));
                } else {
                    collapseButton.active = false;
                }
                elements.add(collapseButton);
                movingX += smallButtonWidth + spacing;

                // Make the key button's label and tooltip
                MutableComponent keyDisplayName;
                if (commandKey.getLimitKey().equals(InputConstants.UNKNOWN)) {
                    keyDisplayName = commandKey.getKey().getDisplayName().copy();
                } else {
                    keyDisplayName = commandKey.getLimitKey().getDisplayName().copy()
                            .append(" + ")
                            .append(commandKey.getKey().getDisplayName());
                }
                MutableComponent label = keyDisplayName;
                Tooltip tooltip = null;
                MutableComponent tooltipComponent = Component.empty();

                boolean internalConflict = false;
                boolean mcConflict = false;
                boolean checkMc = !commandKey.conflictStrategy.state.equals(QuadState.State.THREE);

                if (!commandKey.getLimitKey().equals(InputConstants.UNKNOWN)) {
                    if (!commandKey.profile.commandKeyMap.get(commandKey.getLimitKey()).isEmpty()) {
                        tooltipComponent.append(commandKey.getLimitKey().getDisplayName().copy()
                                .withStyle(ChatFormatting.GOLD));
                        tooltipComponent.append(" is used for another CommandKey.")
                                .withStyle(ChatFormatting.WHITE);
                        internalConflict = true;
                    }
                    KeyMapping conflictKeyM = KeyUtil.getConflict(commandKey.getLimitKey());
                    if (checkMc && conflictKeyM != null) {
                        if (internalConflict) tooltipComponent.append("\n");
                        tooltipComponent.append(commandKey.getLimitKey().getDisplayName().copy()
                                .withStyle(ChatFormatting.RED));
                        tooltipComponent.append(" is also used for: ");
                        tooltipComponent.append(Component.translatable(conflictKeyM.getName())
                                .withStyle(ChatFormatting.GRAY));
                        mcConflict = true;
                    }
                }
                if (!commandKey.getKey().equals(InputConstants.UNKNOWN)) {
                    if (commandKey.profile.commandKeyMap.get(commandKey.getKey()).size() != 1) {
                        if (mcConflict || internalConflict) tooltipComponent.append("\n");
                        tooltipComponent.append(commandKey.getKey().getDisplayName().copy()
                                .withStyle(ChatFormatting.GOLD));
                        tooltipComponent.append(" is used for another CommandKey.")
                                .withStyle(ChatFormatting.WHITE);
                        internalConflict = true;
                    }
                    KeyMapping conflictKeyM = KeyUtil.getConflict(commandKey.getKey());
                    if (checkMc && conflictKeyM != null) {
                        if (mcConflict || internalConflict) tooltipComponent.append("\n");
                        tooltipComponent.append(commandKey.getKey().getDisplayName().copy()
                                .withStyle(ChatFormatting.RED));
                        tooltipComponent.append(" is also used for: ");
                        tooltipComponent.append(Component.translatable(conflictKeyM.getName())
                                .withStyle(ChatFormatting.GRAY));
                        mcConflict = true;
                    }
                }

                if (mcConflict) {
                    label = Component.literal("[ ")
                            .append(keyDisplayName.withStyle(ChatFormatting.WHITE))
                            .append(" ]").withStyle(ChatFormatting.RED);
                    tooltip = Tooltip.create(tooltipComponent
                            .append("\nConflict Strategy: ")
                            .append(switch(commandKey.conflictStrategy.state) {
                                case ZERO -> Component.literal("Submit").withStyle(ChatFormatting.GREEN);
                                case ONE -> Component.literal("Assert").withStyle(ChatFormatting.GOLD);
                                case TWO -> Component.literal("Veto").withStyle(ChatFormatting.RED);
                                // case THREE should never occur since
                                // mcConflict is only checked if not case THREE.
                                case THREE -> Component.literal("Avoid").withStyle(ChatFormatting.DARK_AQUA);
                            }));
                }
                else if (internalConflict) {
                    label = Component.literal("[ ")
                            .append(keyDisplayName.withStyle(ChatFormatting.WHITE))
                            .append(" ]").withStyle(ChatFormatting.GOLD);
                    tooltip = Tooltip.create(tooltipComponent);
                }

                elements.add(Button.builder(label,
                                (button) -> {
                                    listWidget.selectedCommandKey = commandKey;
                                    button.setMessage(Component.literal("> ")
                                            .append(keyDisplayName.withStyle(ChatFormatting.WHITE)
                                                    .withStyle(ChatFormatting.UNDERLINE))
                                            .append(" <").withStyle(ChatFormatting.YELLOW));
                                })
                        .tooltip(tooltip)
                        .pos(movingX, 0)
                        .size(largeButtonWidth, height)
                        .build());
                movingX += largeButtonWidth + spacing;

                CycleButton<QuadState.State> conflictStrategyButton = CycleButton.<QuadState.State>builder(
                                (status) -> switch(status) {
                                    case ZERO -> Component.literal("Submit").withStyle(ChatFormatting.GREEN);
                                    case ONE -> Component.literal("Assert").withStyle(ChatFormatting.GOLD);
                                    case TWO -> Component.literal("Veto").withStyle(ChatFormatting.RED);
                                    case THREE -> Component.literal("Avoid").withStyle(ChatFormatting.DARK_AQUA);
                                })
                        .withValues(QuadState.State.values())
                        .withInitialValue(commandKey.conflictStrategy.state)
                        .withTooltip((status) -> Tooltip.create(Component.literal(switch(status) {
                            case ZERO -> "If the key is already used by Minecraft, this keybind will be cancelled.";
                            case ONE -> "If the key is already used by Minecraft, this keybind will be activated " +
                                    "first, then the other keybind.";
                            case TWO -> "If the key is already used by Minecraft, the other keybind will be " +
                                    "cancelled.\nNote: Some keys (such as movement keys) cannot be cancelled.";
                            case THREE -> "This keybind will not function in-game, but can be activated by pressing " +
                                    "the key while on this screen (if nothing is selected).";
                        })))
                        .create(movingX, 0, largeButtonWidth, height, Component.literal("Conflict"),
                                (button, status) -> {
                                    commandKey.conflictStrategy.state = status;
                                    listWidget.reload();
                                });
                conflictStrategyButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(conflictStrategyButton);
                movingX += largeButtonWidth + spacing;

                int sendStrategyButtonWidth = width - (movingX - x) - smallButtonWidth - spacing;
                if (commandKey.sendStrategy.state.equals(TriState.State.ZERO)) {
                    sendStrategyButtonWidth -= (smallButtonWidth * 2 + 2);
                } else if (commandKey.sendStrategy.state.equals(TriState.State.TWO)) {
                    sendStrategyButtonWidth -= (smallButtonWidth + 2);
                }
                CycleButton<TriState.State> sendStrategyButton;
                if (checkMc) {
                    sendStrategyButton = CycleButton.<TriState.State>builder(
                                    (status) -> switch(status) {
                                        case ZERO -> Component.literal("Send").withStyle(ChatFormatting.GREEN);
                                        case ONE -> Component.literal("Type").withStyle(ChatFormatting.GOLD);
                                        case TWO -> Component.literal("Cycle").withStyle(ChatFormatting.DARK_AQUA);
                                    })
                            .withValues(TriState.State.values())
                            .withInitialValue(commandKey.sendStrategy.state)
                            .withTooltip((status) -> Tooltip.create(Component.literal(switch(status) {
                                case ZERO -> "All messages will be sent.";
                                case ONE -> "The first message will by typed in chat, but not sent.";
                                case TWO -> "Messages will be cycled through, one per key-press. " +
                                        "\nIn this mode, you can send multiple messages in a single " +
                                        "key-press by separating them with two commas e.g. /lobby,,/nick";
                            })))
                            .create(movingX, 0, sendStrategyButtonWidth, height, Component.literal("Mode"),
                                    (button, status) -> {
                                        commandKey.sendStrategy.state = status;
                                        listWidget.reload();
                                    });
                }
                else {
                    sendStrategyButton = CycleButton.<TriState.State>builder(
                                    (status) -> Component.literal("Send").withStyle(ChatFormatting.GREEN))
                            .withValues(TriState.State.values())
                            .withInitialValue(TriState.State.ZERO)
                            .withTooltip((status) -> Tooltip.create(Component.literal("Mode: ")
                                            .append(Component.literal("Send ").withStyle(ChatFormatting.GREEN))
                                            .append(Component.literal("required for Conflict: "))
                                            .append(Component.literal("Avoid").withStyle(ChatFormatting.DARK_AQUA))))
                            .create(movingX, 0, sendStrategyButtonWidth, height, Component.literal("Mode"),
                                    (button, status) -> {});
                    sendStrategyButton.active = false;
                }
                sendStrategyButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(sendStrategyButton);
                if (commandKey.sendStrategy.state.equals(TriState.State.ZERO)) {
                    // Delay field
                    EditBox spaceField = new EditBox(Minecraft.getInstance().font,
                            movingX + sendStrategyButtonWidth + 2, 0, smallButtonWidth * 2,
                            height, Component.empty());
                    spaceField.setMaxLength(4);
                    spaceField.setValue(String.valueOf(commandKey.spaceTicks));
                    spaceField.setResponder((value) -> {
                        try {
                            int ticks = Integer.parseInt(value);
                            if (ticks < 0) ticks = 0;
                            commandKey.spaceTicks = ticks;
                        } catch (NumberFormatException ignored) {}
                    });
                    spaceField.setTooltip(Tooltip.create(Component.literal(
                            "Delay in ticks between consecutive commands")));
                    spaceField.setTooltipDelay(Duration.ofMillis(500));
                    elements.add(spaceField);
                }
                else if (commandKey.sendStrategy.state.equals(TriState.State.TWO)) {
                    // Cycle index button
                    ArrayList<Integer> values = new ArrayList<>();
                    for (int i = 0; i < commandKey.messages.size(); i++) values.add(i);
                    if (values.isEmpty()) values.add(0);
                    if (commandKey.cycleIndex > commandKey.messages.size() - 1) commandKey.cycleIndex = 0;

                    CycleButton<Integer> cycleIndexButton = CycleButton.<Integer>builder(
                            (status) -> Component.literal(status.toString()))
                            .withValues(values)
                            .withInitialValue(commandKey.cycleIndex)
                            .displayOnlyValue()
                            .withTooltip((status) -> Tooltip.create(
                                    Component.literal("The index of the next message to be sent")))
                            .create(movingX + sendStrategyButtonWidth + 2, 0, smallButtonWidth, height,
                                    Component.empty(),
                                    (button, status) -> commandKey.cycleIndex = status);
                    cycleIndexButton.setTooltipDelay(Duration.ofMillis(500));
                    elements.add(cycleIndexButton);
                }

                // Now switch to right-justified

                Button removeButton = Button.builder(Component.literal("\u274C")
                                        .withStyle(ChatFormatting.RED),
                                (button) -> {
                                    listWidget.profile.removeCmdKey(commandKey);
                                    listWidget.reload();
                                })
                        .pos(x + width - smallButtonWidth, 0)
                        .size(smallButtonWidth, height)
                        .build();
                removeButton.setTooltip(Tooltip.create(Component.literal("Remove command key")));
                removeButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(removeButton);
            }
        }

        private static class CommandKeyMessageTeaserEntry extends Entry {
            ProfileEditList listWidget;
            CommandKey commandKey;

            CommandKeyMessageTeaserEntry(int x, int width, int height, ProfileEditList listWidget,
                                         CommandKey commandKey) {
                super();
                this.listWidget = listWidget;
                this.commandKey = commandKey;

                int spacing = 5;
                int smallButtonWidth = 20;
                int moveButtonWidth = 12;
                int messageFieldWidth = width - smallButtonWidth * 2 - moveButtonWidth * 2 - spacing * 3;

                // Switch to right-justified

                String message = commandKey.messages.isEmpty() ? "" : commandKey.messages.get(0);
                if (commandKey.messages.size() > 1) {
                    message = message + " [+" + (commandKey.messages.size() - 1) + "]";
                }
                message = message + " [Click to Expand]";

                EditBoxPreview messagePreview = new EditBoxPreview(Minecraft.getInstance().font,
                        x + width - smallButtonWidth - spacing - messageFieldWidth, 0,
                        messageFieldWidth, height, Component.empty());
                messagePreview.setMaxLength(300);
                messagePreview.setValue(message);
                elements.add(messagePreview);
            }

            private class EditBoxPreview extends EditBox {

                public EditBoxPreview(Font font, int x, int y, int width, int height, Component label) {
                    super(font, x, y, width, height, label);
                }

                @Override
                public void onClick(double mouseX, double mouseY) {
                    listWidget.expandedKeys.add(commandKey);
                    listWidget.reload();
                }
            }
        }

        private static class CommandKeyMessageEntry extends Entry {
            ProfileEditList listWidget;
            CommandKey commandKey;

            CommandKeyMessageEntry(int x, int width, int height, ProfileEditList listWidget,
                                   CommandKey commandKey, int index) {
                super();
                this.listWidget = listWidget;
                this.commandKey = commandKey;

                int spacing = 5;
                int smallButtonWidth = 20;
                int moveButtonWidth = 12;
                int messageFieldWidth = width - smallButtonWidth * 2 - spacing * 2;

                Button upButton = Button.builder(Component.literal("\u2191"),
                                (button) -> {
                                    if (Screen.hasShiftDown()) {
                                        if (index > 0) {
                                            commandKey.messages.add(0, commandKey.messages.get(index));
                                            commandKey.messages.remove(index + 1);
                                            listWidget.reload();
                                        }
                                    } else {
                                        if (index > 0) {
                                            String temp = commandKey.messages.get(index);
                                            commandKey.messages.set(index, commandKey.messages.get(index - 1));
                                            commandKey.messages.set(index - 1, temp);
                                            listWidget.reload();
                                        }
                                    }
                                })
                        .pos(x, 0)
                        .size(moveButtonWidth, height)
                        .build();
                if (index > 0) {
                    upButton.setTooltip(Tooltip.create(Component.literal(
                            "Click to move up.\nShift-Click to send all the way.")));
                    upButton.setTooltipDelay(Duration.ofMillis(1000));
                } else {
                    upButton.active = false;
                }
                elements.add(upButton);

                Button downButton = Button.builder(Component.literal("\u2193"),
                                (button) -> {
                                    if (Screen.hasShiftDown()) {
                                        if (index < commandKey.messages.size() - 1) {
                                            commandKey.messages.add(commandKey.messages.get(index));
                                            commandKey.messages.remove(index);
                                            listWidget.reload();
                                        }
                                    } else {
                                        if (index < commandKey.messages.size() - 1) {
                                            String temp = commandKey.messages.get(index);
                                            commandKey.messages.set(index, commandKey.messages.get(index + 1));
                                            commandKey.messages.set(index + 1, temp);
                                            listWidget.reload();
                                        }
                                    }
                                })
                        .pos(x + moveButtonWidth, 0)
                        .size(moveButtonWidth, height)
                        .build();
                if (index < commandKey.messages.size() - 1) {
                    downButton.setTooltip(Tooltip.create(Component.literal(
                            "Click to move down.\nShift-Click to send all the way.")));
                    downButton.setTooltipDelay(Duration.ofMillis(1000));
                } else {
                    downButton.active = false;
                }
                elements.add(downButton);

                EditBox messageField = new EditBox(Minecraft.getInstance().font,
                        x + smallButtonWidth + spacing, 0,
                        messageFieldWidth, height, Component.empty());
                messageField.setMaxLength(255);
                messageField.setValue(commandKey.messages.get(index));
                messageField.setResponder((value) -> commandKey.messages.set(index, value.stripLeading()));
                elements.add(messageField);

                // Switch to right-justified

                Button removeButton = Button.builder(Component.literal("\u274C"),
                                (button) -> {
                                    commandKey.messages.remove(index);
                                    if (commandKey.messages.isEmpty()) {
                                        listWidget.expandedKeys.remove(commandKey);
                                    }
                                    listWidget.reload();
                                })
                        .pos(x + width - smallButtonWidth, 0)
                        .size(smallButtonWidth, height)
                        .build();
                removeButton.setTooltip(Tooltip.create(Component.literal("Remove message")));
                removeButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(removeButton);
            }
        }
    }
}
