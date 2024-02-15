package notryken.commandkeys.gui.component.listwidget;

import com.mojang.blaze3d.platform.InputConstants;
import io.netty.channel.local.LocalAddress;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import notryken.commandkeys.CommandKeys;
import notryken.commandkeys.config.CommandKey;
import notryken.commandkeys.config.Profile;
import notryken.commandkeys.gui.screen.ConfigScreen;

import java.net.SocketAddress;

public class ProfileListWidget extends ConfigListWidget {
    Profile profile;
    CommandKey selectedCommandKey;
    InputConstants.Key heldKey;
    
    public ProfileListWidget(Minecraft minecraft, int width, int height, int top, int bottom,
                             int itemHeight, int entryRelX, int entryWidth, int entryHeight,
                             int scrollWidth, Profile profile) {
        super(minecraft, width, height, top, bottom, itemHeight, entryRelX, 
                entryWidth, entryHeight, scrollWidth);
        this.profile = profile;

        addEntry(new Entry.GlobalSettingEntry(entryX, entryWidth, entryHeight, this));

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("------------------------ Command Keys \u2139 ------------------------"),
                Tooltip.create(Component.literal("The messages for each key will be sent if you press the " +
                        "corresponding hotkey while in-game (depending on individual settings).")), 500));

        for (CommandKey commandKey : profile.getCommandKeys()) {
            int size = commandKey.messages.size();
            addEntry(new Entry.CommandKeyFirstFieldEntry(entryX, entryWidth, entryHeight, this,
                    commandKey, size <= 1));
            for (int i = 1; i < commandKey.messages.size(); i++) {
                addEntry(new Entry.CommandKeyFieldEntry(entryX, entryWidth, entryHeight, this,
                        commandKey, i, i == size -1));
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
                entryWidth, entryHeight, scrollWidth, profile);
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
        boolean singleplayer = false;
        SocketAddress address = CommandKeys.activeAddress();
        if (address instanceof LocalAddress) {
            singleplayer = true;
        }
        Screen lastScreen = screen.getLastScreen();
        if (lastScreen instanceof ConfigScreen lastConfigScreen) {
            lastScreen = lastConfigScreen.getLastScreen();
        }
        minecraft.setScreen(new ConfigScreen(lastScreen,
                Component.translatable("screen.commandkeys.title.default"),
                new ProfileSetListWidget(minecraft, screen.width, screen.height, y0, y1,
                        itemHeight, -150, 300, entryHeight, 320,
                        singleplayer, null)));
    }

    public void openMinecraftControlsScreen() {
        minecraft.setScreen(new KeyBindsScreen(screen, Minecraft.getInstance().options));
    }

    public void openCommandKeyScreen(CommandKey commandKey) {
        minecraft.setScreen(new ConfigScreen(screen,
                Component.translatable("screen.commandkeys.title.mono"),
                new CommandKeyListWidget(minecraft, screen.width, screen.height, y0, y1,
                        itemHeight, -120, 240, entryHeight, 260, commandKey)));
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

        private static class CommandKeyFirstFieldEntry extends Entry {
            CommandKeyFirstFieldEntry(int x, int width, int height, ProfileListWidget listWidget,
                                      CommandKey commandKey, boolean showAdd) {
                super();
                int spacing = 5;
                int smallButtonWidth = 20;
                int keyButtonWidth = 75;
                int messageFieldWidth = width - smallButtonWidth * 4 - keyButtonWidth - spacing * 5;
                int movingX = x;

                elements.add(new ImageButton(movingX, 0, smallButtonWidth, height,
                        0, 0, 20, ConfigListWidget.Entry.CONFIGURATION_ICON,
                        32, 64, (button) -> listWidget.openCommandKeyScreen(commandKey),
                        Component.literal("options")));
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
//                    KeyMapping conflictKeyM = KeyUtil.getConflictKeyMapping(commandKey.getLimitKey());
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
//                    KeyMapping conflictKeyM = KeyUtil.getConflictKeyMapping(commandKey.getKey());
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
                                case ZERO -> Component.literal("Submissive").withStyle(ChatFormatting.GREEN);
                                case ONE -> Component.literal("Assertive").withStyle(ChatFormatting.GOLD);
                                case TWO -> Component.literal("Aggressive").withStyle(ChatFormatting.RED);
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
                        .size(keyButtonWidth, height)
                        .build());
                movingX += keyButtonWidth + spacing;

                EditBox messageField = new EditBox(Minecraft.getInstance().font, movingX, 0,
                        messageFieldWidth, height, Component.literal("Message"));
                messageField.setMaxLength(255);
                messageField.setValue(commandKey.messages.isEmpty() ? "" : commandKey.messages.get(0));
                messageField.setResponder(
                        (value) -> {
                            if (commandKey.messages.isEmpty()) {
                                commandKey.messages.add(value.stripLeading());
                            } else {
                                commandKey.messages.set(0, value.stripLeading());
                            }
                        });
                elements.add(messageField);

                // Now switch to right-justified

                Button upButton = Button.builder(Component.literal("\u2191"),
                                (button) -> {})
                        .pos(x + width - spacing * 2 - smallButtonWidth * 2 - 24, 0)
                        .size(12, height)
                        .build();
                upButton.active = false;
                elements.add(upButton);

                Button downButton = Button.builder(Component.literal("\u2193"),
                                (button) -> {
                                    int index = 0;
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
                                    }})
                        .pos(x + width - spacing * 2 - smallButtonWidth * 2 - 12, 0)
                        .size(12, height)
                        .build();
                if (commandKey.messages.size() > 1) {
                    downButton.setTooltip(Tooltip.create(Component.literal(
                            "Click to move down.\nShift-Click to send all the way.")));
                    downButton.setTooltipDelay(1000);
                }
                else {
                    downButton.active = false;
                }
                elements.add(downButton);

                Button removeButton = Button.builder(Component.literal("\u274C"),
                                (button) -> {
                                    if (commandKey.messages.size() <= 1) {
                                        listWidget.profile.removeCmdKey(commandKey);
                                    } else {
                                        commandKey.messages.remove(0);
                                    }
                                    listWidget.reload();
                                })
                        .pos(x + width - spacing - smallButtonWidth * 2, 0)
                        .size(smallButtonWidth, height)
                        .build();
                removeButton.setTooltip(Tooltip.create(Component.literal(
                        commandKey.messages.size() <= 1 ? "Remove command key" : "Remove message")));
                removeButton.setTooltipDelay(500);
                elements.add(removeButton);

                if (showAdd) {
                    Button addButton = Button.builder(Component.literal("+"),
                                    (button) -> {
                                        if (commandKey.messages.isEmpty()) commandKey.messages.add("");
                                        commandKey.messages.add("");
                                        listWidget.reload();
                                    })
                            .pos(x + width - smallButtonWidth, 0)
                            .size(smallButtonWidth, height)
                            .build();
                    addButton.setTooltip(Tooltip.create(Component.literal("New message")));
                    addButton.setTooltipDelay(500);
                    elements.add(addButton);
                }
            }
        }

        private static class CommandKeyFieldEntry extends Entry {
            CommandKeyFieldEntry(int x, int width, int height, ProfileListWidget listWidget,
                                 CommandKey commandKey, int index, boolean showAdd) {
                super();
                int spacing = 5;
                int smallButtonWidth = 20;
                int keyButtonWidth = 75;
                int messageFieldWidth = width - smallButtonWidth * 4 - keyButtonWidth - spacing * 5;
                int movingX = x + smallButtonWidth + spacing + keyButtonWidth + spacing;

                EditBox messageField = new EditBox(Minecraft.getInstance().font, movingX, 0,
                        messageFieldWidth, height, Component.literal("Message"));
                messageField.setMaxLength(255);
                messageField.setValue(commandKey.messages.get(index));
                messageField.setResponder((value) -> commandKey.messages.set(index, value.stripLeading()));
                elements.add(messageField);

                // Now switch to right-justified

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
                                    }})
                        .pos(x + width - spacing * 2 - smallButtonWidth * 2 - 24, 0)
                        .size(12, height)
                        .build();
                upButton.setTooltip(Tooltip.create(Component.literal(
                        "Click to move up.\nShift-Click to send all the way.")));
                upButton.setTooltipDelay(1000);
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
                                    }})
                        .pos(x + width - spacing * 2 - smallButtonWidth * 2 - 12, 0)
                        .size(12, height)
                        .build();
                if (index < commandKey.messages.size() - 1) {
                    downButton.setTooltip(Tooltip.create(Component.literal(
                            "Click to move down.\nShift-Click to send all the way.")));
                    downButton.setTooltipDelay(1000);
                } else {
                    downButton.active = false;
                }
                elements.add(downButton);

                Button removeButton = Button.builder(Component.literal("\u274C"),
                                (button) -> {
                                    commandKey.messages.remove(index);
                                    listWidget.reload();
                                })
                        .pos(x + width - spacing - smallButtonWidth * 2, 0)
                        .size(smallButtonWidth, height)
                        .build();
                removeButton.setTooltip(Tooltip.create(Component.literal(
                        commandKey.messages.size() <= 1 ? "Remove command key" : "Remove message")));
                removeButton.setTooltipDelay(500);
                elements.add(removeButton);

                if (showAdd) {
                    Button addButton = Button.builder(Component.literal("+"),
                                    (button) -> {
                                        commandKey.messages.add("");
                                        listWidget.reload();
                                    })
                            .pos(x + width - smallButtonWidth, 0)
                            .size(smallButtonWidth, height)
                            .build();
                    addButton.setTooltip(Tooltip.create(Component.literal("New message")));
                    addButton.setTooltipDelay(500);
                    elements.add(addButton);
                }
            }
        }
    }
}
