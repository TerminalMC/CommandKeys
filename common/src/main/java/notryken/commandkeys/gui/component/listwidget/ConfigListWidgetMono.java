package notryken.commandkeys.gui.component.listwidget;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import notryken.commandkeys.CommandKeys;
import notryken.commandkeys.config.MsgKeyMapping;
import notryken.commandkeys.gui.screen.ConfigScreenMono;

import java.util.List;

public class ConfigListWidgetMono extends ConfigListWidget {

    public MsgKeyMapping selectedMsgKey;

    public ConfigListWidgetMono(Minecraft client, int width, int height, int top, int bottom,
                                int itemHeight, Screen parentScreen, Component title) {
        super(client, width, height, top, bottom, itemHeight, parentScreen, title);

        addEntry(new Entry.SettingToggles(width, this));

        addEntry(new ConfigListWidget.Entry.Header(width, client, Component.nullToEmpty(
                "--------------------- Mono-Hotkey Messages \u2139 ---------------------"),
                Component.literal("These messages will be sent if you press the corresponding " +
                        "hotkey while in-game, provided no other function is bound to the same key.")));

        addEntry(new Entry.ListHeader(width, client));

        List<MsgKeyMapping> msgKeyList = CommandKeys.config().getMsgKeyListMono();

        for (MsgKeyMapping e : msgKeyList) {
            addEntry(new Entry.KeyMessageField(width, this, client, e));
        }
        addEntry(new Entry.AddMessageButton(width, this));
    }

    public boolean keyPressed(int keyCode, int scanCode) {
        return selectedMsgKey == null;
    }

    public boolean keyReleased(int keyCode, int scanCode) {
        if (selectedMsgKey != null) {
            if (keyCode == 256) {
                setKeyCode(selectedMsgKey, InputConstants.UNKNOWN);
            } else {
                setKeyCode(selectedMsgKey, InputConstants.getKey(keyCode, scanCode));
            }
            return true;
        }
        return false;
    }

    protected void setKeyCode(MsgKeyMapping msgKey, InputConstants.Key keyCode) {
        msgKey.setKeyCode(keyCode);
        reloadScreen();
    }

    protected void addMessage() {
        if (CommandKeys.config().addMsgKeyMono()) {
            reloadScreen();
        }
    }

    protected void removeMessage(MsgKeyMapping msgKey) {
        if (CommandKeys.config().removeMsgKeyMono(msgKey)) {
            reloadScreen();
        }
    }

    public void reloadScreen() {
        ConfigListWidget listWidget = new ConfigListWidgetMono(
                client, width, height, top, bottom, itemHeight, parentScreen, screenTitle);
        listWidget.setScrollAmount(this.getScrollAmount());
        client.setScreen(new ConfigScreenMono(parentScreen, client.options, screenTitle, listWidget));
    }

    public ConfigListWidgetMono resize(int width, int height, int top, int bottom) {
        ConfigListWidgetMono listWidget = new ConfigListWidgetMono(
                client, width, height, top, bottom, itemHeight, parentScreen, screenTitle);
        listWidget.setScrollAmount(getScrollAmount());
        return listWidget;
    }

    private void openDualConfigScreen() {
        ConfigListWidget listWidget = new ConfigListWidgetDual(
                client, width, height, top, bottom, itemHeight, parentScreen, screenTitle);
        client.setScreen(new ConfigScreenMono(parentScreen, client.options, screenTitle, listWidget));
    }

    public abstract static class Entry extends ConfigListWidget.Entry {

        private static class SettingToggles extends Entry {
            SettingToggles(int width, ConfigListWidgetMono listWidget) {
                options.add(CycleButton.booleanBuilder(Component.nullToEmpty("Yes"),
                                Component.nullToEmpty("No"))
                        .withInitialValue(CommandKeys.config().showHudMessage)
                        .withTooltip((status) -> Tooltip.create(Component.nullToEmpty(
                                "Briefly show the sent message/command as a pop-up above the hotbar.")))
                        .create(width / 2 - 200, 0, 120, 20,
                                Component.literal("Show HUD Display"),
                                (button, status) -> CommandKeys.config().showHudMessage = status));

                options.add(CycleButton.booleanBuilder(Component.nullToEmpty("Yes"),
                                Component.nullToEmpty("No"))
                        .withInitialValue(CommandKeys.config().addToHistory)
                        .withTooltip((status) -> Tooltip.create(Component.nullToEmpty(
                                "Add sent messages/commands to history.")))
                        .create(width / 2 - 75, 0, 120, 20,
                                Component.literal("Add to History"),
                                (button, status) -> CommandKeys.config().addToHistory = status));
                options.add(Button.builder(Component.literal("Dual-Key Options"),
                                (button) -> listWidget.openDualConfigScreen())
                        .pos(width / 2 + 80, 0)
                        .size(120, 20)
                        .build());
            }
        }

        public static class ListHeader extends Entry {
            public ListHeader(int width, Minecraft client) {
                StringWidget keyHeader = new StringWidget(width / 2 - 200, 0, 80, 20,
                        Component.nullToEmpty("Hotkey"), client.font);
                StringWidget messageHeader = new StringWidget(width / 2 - 110, 0, 280, 20,
                        Component.nullToEmpty("Message/Command \u2139"), client.font);
                messageHeader.setTooltip(Tooltip.create(Component.literal("You can list multiple " +
                        "commands to be sent sequentially by separating them with a pair of commas. " +
                        "Example: /gamemode creative,,/effect clear @s")));
                messageHeader.setTooltipDelay(500);
                options.add(keyHeader);
                options.add(messageHeader);
            }
        }

        private static class KeyMessageField extends Entry {

            KeyMessageField(int width, ConfigListWidgetMono listWidget, Minecraft client,
                            MsgKeyMapping msgKey) {

                MutableComponent keyName = msgKey.keyCode.getDisplayName().copy();
                MutableComponent label = keyName;
                Tooltip tooltip = null;

                if (msgKey.label != null) {
                    label = msgKey.label;
                    tooltip = msgKey.tooltip;
                }

                options.add(
                        Button.builder(label, (button) -> {
                            listWidget.selectedMsgKey = msgKey;
                            button.setMessage(Component.literal("> ")
                                    .append(keyName.copy().withStyle(ChatFormatting.WHITE)
                                            .withStyle(ChatFormatting.UNDERLINE))
                                    .append(" <").withStyle(ChatFormatting.YELLOW));
                        })
                                .tooltip(tooltip)
                                .size(80, 20)
                                .pos(width / 2 - 200, 0)
                                .build());

                EditBox messageField = new EditBox(client.font, width / 2 - 110, 0, 280, 20,
                        Component.literal("Message"));
                messageField.setMaxLength(1024);
                messageField.setValue(msgKey.msg);
                messageField.setResponder((value) -> msgKey.msg = value.strip());
                options.add(messageField);

                options.add(Button.builder(Component.literal("X"),
                                (button) -> listWidget.removeMessage(msgKey))
                        .size(20, 20)
                        .pos(width / 2 + 180, 0)
                        .build());
            }
        }

        private static class AddMessageButton extends Entry {
            public AddMessageButton(int width, ConfigListWidgetMono listWidget) {
                options.add(Button.builder(Component.literal("+"),
                                (button) -> listWidget.addMessage())
                        .size(400, 20)
                        .pos(width / 2 - 200, 0)
                        .build());
            }
        }
    }

}