package notryken.commandkeys.gui.component.listwidget;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import notryken.commandkeys.CommandKeys;
import notryken.commandkeys.config.CommandDualKey;
import notryken.commandkeys.gui.screen.ConfigScreen;
import notryken.commandkeys.util.SendingUtil;

import static notryken.commandkeys.CommandKeys.config;

public class DualKeySetListWidget extends ConfigListWidget {
    CommandDualKey selectedCommandKey;

    public DualKeySetListWidget(Minecraft minecraft, int width, int height, int top, int bottom,
                                int itemHeight, int entryRelX, int entryWidth, int entryHeight,
                                int scrollWidth) {
        super(minecraft, width, height, top, bottom, itemHeight, entryRelX,
                entryWidth, entryHeight, scrollWidth);

        addEntry(new Entry.GlobalSettingEntry(entryX, entryWidth, entryHeight, this));

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("--------------------- Command Dual-Keys \u2139 ---------------------"),
                Tooltip.create(Component.literal("If you opened this screen by pressing the config key " +
                        "in-game, you can now send one of the messages (or sets of messages) below by pressing " +
                        "the corresponding key on your keyboard.")), 500));

        for (CommandDualKey commandKey : config().getDualKeySet()) {
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
                    config().addDualKey(new CommandDualKey());
                    reload();
                }));
    }

    @Override
    public ConfigListWidget resize(int width, int height, int top, int bottom,
                                   int itemHeight, double scrollAmount) {
        DualKeySetListWidget newListWidget = new DualKeySetListWidget(
                minecraft, width, height, top, bottom, itemHeight,
                entryRelX, entryWidth, entryHeight, scrollWidth);
        newListWidget.setScrollAmount(scrollAmount);
        return newListWidget;
    }

    @Override
    public boolean willHandleKey(InputConstants.Key key) {
        return ((getSelected() == null &&
                    key.getValue() != InputConstants.KEY_ESCAPE &&
                    !key.getType().equals(InputConstants.Type.MOUSE)) ||
                selectedCommandKey != null);
    }

    @Override
    public boolean handleKey(InputConstants.Key key) {
        if (!key.getType().equals(InputConstants.Type.MOUSE)) {
            if (selectedCommandKey == null) {
                if (getSelected() == null) {
                    CommandDualKey commandKey = CommandDualKey.MAP.get(key);
                    if (commandKey != null && !commandKey.messages.isEmpty() &&
                            minecraft.getConnection() != null &&
                            minecraft.getConnection().isAcceptingMessages()) {
                        screen.onClose(); // TODO this is a save idea, idk about it
                        minecraft.setScreen(null);
                        for (String message : commandKey.messages) {
                            SendingUtil.send(message, config().dualAddToHistory, config().dualShowHudMessage);
                        }
                        return true;
                    }
                }
            }
            else {
                if (key.getValue() == InputConstants.KEY_ESCAPE) {
                    selectedCommandKey.setKey(InputConstants.UNKNOWN);
                }
                else if (!key.equals(CommandKeys.CONFIG_KEY.key)) { // ((KeyMappingAccessor) CommandKeys.CONFIG_KEY).getKey()
                    selectedCommandKey.setKey(key);
                }
                reload();
                return true;
            }
        }
        return false;
    }

    public void openMonoKeyScreen() {
        minecraft.setScreen(new ConfigScreen(screen.getLastScreen(),
                Component.translatable("screen.commandkeys.title.mono"),
                new MonoKeySetListWidget(minecraft, screen.width, screen.height, y0, y1,
                        itemHeight, entryRelX, entryWidth, entryHeight, scrollWidth)));
    }

    private abstract static class Entry extends ConfigListWidget.Entry {

        private static class GlobalSettingEntry extends Entry {
            GlobalSettingEntry(int x, int width, int height, DualKeySetListWidget listWidget) {
                super();
                int spacing = 5;
                int buttonWidth = (width - spacing * 8) / 3;
                elements.add(CycleButton.booleanBuilder(
                                Component.literal("Yes").withStyle(ChatFormatting.GREEN),
                                Component.literal("No").withStyle(ChatFormatting.RED))
                        .withInitialValue(config().dualAddToHistory)
                        .withTooltip((status) -> Tooltip.create(Component.nullToEmpty(
                                "Add sent messages/commands to history.")))
                        .create(x, 0, buttonWidth, height,
                                Component.literal("Add to History"),
                                (button, status) -> config().dualAddToHistory = status));
                elements.add(CycleButton.booleanBuilder(
                        Component.literal("Yes").withStyle(ChatFormatting.GREEN),
                                Component.literal("No").withStyle(ChatFormatting.RED))
                        .withInitialValue(config().dualShowHudMessage)
                        .withTooltip((status) -> Tooltip.create(Component.nullToEmpty(
                                "Briefly show the sent message/command as a pop-up above the hotbar.")))
                        .create(x + buttonWidth + spacing, 0, buttonWidth, height,
                                Component.literal("Show HUD Display"),
                                (button, status) -> config().dualShowHudMessage = status));
                elements.add(Button.builder(Component.literal("Mono-Key Options"),
                                (button) -> listWidget.openMonoKeyScreen())
                        .pos(x + width - buttonWidth, 0)
                        .size(buttonWidth, height)
                        .build());
            }
        }

        private static class CommandKeyFirstFieldEntry extends Entry {
            CommandKeyFirstFieldEntry(int x, int width, int height, DualKeySetListWidget listWidget,
                                      CommandDualKey commandKey, boolean showAdd) {
                super();
                int spacing = 5;
                int smallButtonWidth = 20;
                int keyButtonWidth = 100;
                int messageFieldWidth = width - smallButtonWidth * 3 - keyButtonWidth - spacing * 4;
                int movingX = x;

                MutableComponent keyName = commandKey.getKey().getDisplayName().copy();
                elements.add(Button.builder(keyName,
                                (button) -> {
                                    listWidget.selectedCommandKey = commandKey;
                                    button.setMessage(Component.literal("> ")
                                            .append(keyName.withStyle(ChatFormatting.WHITE)
                                                    .withStyle(ChatFormatting.UNDERLINE))
                                            .append(" <").withStyle(ChatFormatting.YELLOW));
                                })
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
                                commandKey.messages.add(value.strip());
                            } else {
                                commandKey.messages.set(0, value.strip());
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
                                        config().removeDualKey(commandKey);
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
            CommandKeyFieldEntry(int x, int width, int height, DualKeySetListWidget listWidget,
                                 CommandDualKey commandKey, int index, boolean showAdd) {
                super();
                int spacing = 5;
                int smallButtonWidth = 20;
                int keyButtonWidth = 100;// Not used but required for offset
                int messageFieldWidth = width - smallButtonWidth * 3 - keyButtonWidth - spacing * 4;
                int movingX = x + keyButtonWidth + spacing;

                EditBox messageField = new EditBox(Minecraft.getInstance().font, movingX, 0,
                        messageFieldWidth, height, Component.literal("Message"));
                messageField.setMaxLength(255);
                messageField.setValue(commandKey.messages.get(index));
                messageField.setResponder((value) -> commandKey.messages.set(index, value.strip()));
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
