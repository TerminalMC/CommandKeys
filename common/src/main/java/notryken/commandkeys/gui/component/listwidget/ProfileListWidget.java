package notryken.commandkeys.gui.component.listwidget;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import notryken.commandkeys.CommandKeys;
import notryken.commandkeys.config.CommandKey;
import notryken.commandkeys.config.Profile;
import notryken.commandkeys.config.TriState;
import notryken.commandkeys.gui.screen.ConfigScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ProfileListWidget extends ConfigListWidget {
    Profile profile;
    Set<CommandKey> expandedKeys;
    CommandKey selectedCommandKey;
    InputConstants.Key heldKey;
    
    public ProfileListWidget(Minecraft minecraft, int width, int height, int top, int bottom,
                             int itemHeight, int entryRelX, int entryWidth, int entryHeight,
                             int scrollWidth, @NotNull Profile profile,
                             @Nullable Set<CommandKey> expandedKeys) {
        super(minecraft, width, height, top, bottom, itemHeight, entryRelX,
                entryWidth, entryHeight, scrollWidth);
        this.profile = profile;
        this.expandedKeys = (expandedKeys == null) ? new HashSet<>() : expandedKeys;

        addEntry(new Entry.GlobalSettingEntry(entryX, entryWidth, entryHeight, this));

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("------------------------ Command Keys \u2139 ------------------------"),
                Tooltip.create(Component.literal("The messages for each key will be sent if you press the " +
                        "corresponding hotkey while in-game (depending on individual settings).")), 500));

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
                addEntry(new ConfigListWidget.Entry.ActionButtonEntry(entryX + 25, 0, entryWidth - 50,
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
        addEntry(new ConfigListWidget.Entry.ActionButtonEntry(entryX, 0, entryWidth, entryHeight,
                Component.literal("+"), null, -1,
                (button) -> {
                    profile.addCmdKey(new CommandKey());
                    reload();
                }));
    }

    @Override
    public ConfigListWidget resize(int width, int height, int top, int bottom, 
                                   int itemHeight, double scrollAmount) {
        ProfileListWidget newListWidget = new ProfileListWidget(
                minecraft, width, height, top, bottom, itemHeight, entryRelX,
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
            else if (!key.equals(CommandKeys.CONFIG_KEY.key)) {
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
            }
            else {
                return false;
            }
            return true;
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
        if (lastScreen instanceof ConfigScreen lastConfigScreen) {
            lastScreen = lastConfigScreen.getLastScreen();
        }
        minecraft.setScreen(new ConfigScreen(lastScreen,
                Component.translatable("screen.commandkeys.title.profiles"),
                new ProfileSetListWidget(minecraft, screen.width, screen.height, y0, y1,
                        itemHeight, -180, 360, entryHeight, 380, null)));
    }

    public void openMinecraftControlsScreen() {
        minecraft.setScreen(new KeyBindsScreen(screen, Minecraft.getInstance().options));
    }

    private abstract static class Entry extends ConfigListWidget.Entry {

        private static class GlobalSettingEntry extends Entry {
            GlobalSettingEntry(int x, int width, int height, ProfileListWidget listWidget) {
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
                historyButton.setTooltipDelay(500);
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
                displayButton.setTooltipDelay(500);
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
            CommandKeyOptionsEntry(int x, int width, int height, ProfileListWidget listWidget,
                                   CommandKey commandKey) {
                super();
                int spacing = 5;
                int smallButtonWidth = 20;
                int largeButtonWidth = (width - smallButtonWidth * 2 - spacing * 4) / 3;
                int movingX = x;

                ImageButton collapseButton = new ImageButton(movingX, 0, smallButtonWidth, height,
                        0, 0, 20, ConfigListWidget.Entry.COLLAPSE_ICON, 32, 64,
                        (button) -> {
                            listWidget.expandedKeys.remove(commandKey);
                            listWidget.reload();
                        },
                        Component.empty());
                if (listWidget.expandedKeys.contains(commandKey)) {
                    collapseButton.setTooltip(Tooltip.create(Component.literal("Collapse")));
                    collapseButton.setTooltipDelay(500);
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
                boolean conflict = false;
                if (!commandKey.getLimitKey().equals(InputConstants.UNKNOWN)) {
                    KeyMapping conflictKeyM = KeyMapping.MAP.get(commandKey.getLimitKey());
                    if (conflictKeyM != null) {
                        tooltipComponent.append(commandKey.getLimitKey().getDisplayName().copy()
                                .withStyle(ChatFormatting.RED));
                        tooltipComponent.append(" is also used for: ");
                        tooltipComponent.append(Component.translatable(conflictKeyM.getName())
                                .withStyle(ChatFormatting.GRAY));
                        conflict = true;
                    }
                }
                if (!commandKey.getKey().equals(InputConstants.UNKNOWN)) {
                    KeyMapping conflictKeyM = KeyMapping.MAP.get(commandKey.getKey());
                    if (conflictKeyM != null) {
                        if (conflict) tooltipComponent.append("\n");
                        tooltipComponent.append(commandKey.getKey().getDisplayName().copy()
                                .withStyle(ChatFormatting.RED));
                        tooltipComponent.append(" is also used for: ");
                        tooltipComponent.append(Component.translatable(conflictKeyM.getName())
                                .withStyle(ChatFormatting.GRAY));
                        conflict = true;
                    }
                }
                if (conflict) {
                    label = Component.literal("[ ")
                            .append(keyDisplayName.withStyle(ChatFormatting.WHITE))
                            .append(" ]").withStyle(ChatFormatting.RED);
                    tooltip = Tooltip.create(tooltipComponent
                            .append("\nConflict Strategy: ")
                            .append(switch(commandKey.conflictStrategy.state) {
                                case ZERO -> Component.literal("Submit").withStyle(ChatFormatting.GREEN);
                                case ONE -> Component.literal("Assert").withStyle(ChatFormatting.GOLD);
                                case TWO -> Component.literal("Veto").withStyle(ChatFormatting.RED);
                            }));
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

                CycleButton<TriState.State> conflictStrategyButton = CycleButton.<TriState.State>builder(
                                (status) -> switch(status) {
                                    case ZERO -> Component.literal("Submit").withStyle(ChatFormatting.GREEN);
                                    case ONE -> Component.literal("Assert").withStyle(ChatFormatting.GOLD);
                                    case TWO -> Component.literal("Veto").withStyle(ChatFormatting.RED);
                                })
                        .withValues(TriState.State.values())
                        .withInitialValue(commandKey.conflictStrategy.state)
                        .withTooltip((status) -> Tooltip.create(Component.literal(switch(status) {
                            case ZERO -> "If the key is already used by Minecraft, this keybind will be cancelled.";
                            case ONE -> "If the key is already used by Minecraft, this keybind will be activated " +
                                    "first, then the other keybind.";
                            case TWO -> "If the key is already used by Minecraft, the other keybind will be " +
                                    "cancelled.\nNote: Some keys (including movement and sneak) cannot be cancelled.";
                        })))
                        .create(movingX, 0, largeButtonWidth, height, Component.literal("Conflict"),
                                (button, status) -> {
                                    commandKey.conflictStrategy.state = status;
                                    listWidget.reload();
                                });
                conflictStrategyButton.setTooltipDelay(500);
                elements.add(conflictStrategyButton);
                movingX += largeButtonWidth + spacing;

                int sendStrategyButtonWidth = width - (movingX - x) - smallButtonWidth - spacing;
                if (commandKey.sendStrategy.state.equals(TriState.State.TWO)) {
                    sendStrategyButtonWidth -= (smallButtonWidth + 2);
                }
                CycleButton<TriState.State> sendStrategyButton = CycleButton.<TriState.State>builder(
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
                sendStrategyButton.setTooltipDelay(500);
                elements.add(sendStrategyButton);
                // Cycle index button
                if (commandKey.sendStrategy.state.equals(TriState.State.TWO)) {
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
                                    Component.literal("The index of the next message to be sent.")))
                            .create(movingX + sendStrategyButtonWidth + 2, 0, smallButtonWidth, height,
                                    Component.empty(),
                                    (button, status) -> commandKey.cycleIndex = status);
                    cycleIndexButton.setTooltipDelay(500);
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
                removeButton.setTooltipDelay(500);
                elements.add(removeButton);
            }
        }

        private static class CommandKeyMessageTeaserEntry extends Entry {
            ProfileListWidget listWidget;
            CommandKey commandKey;

            CommandKeyMessageTeaserEntry(int x, int width, int height, ProfileListWidget listWidget,
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
            ProfileListWidget listWidget;
            CommandKey commandKey;

            CommandKeyMessageEntry(int x, int width, int height, ProfileListWidget listWidget,
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
                    upButton.setTooltipDelay(1000);
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
                    downButton.setTooltipDelay(1000);
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
                removeButton.setTooltipDelay(500);
                elements.add(removeButton);
            }
        }
    }
}
