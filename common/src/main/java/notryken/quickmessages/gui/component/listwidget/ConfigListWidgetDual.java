package notryken.quickmessages.gui.component.listwidget;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import notryken.quickmessages.QuickMessages;
import notryken.quickmessages.gui.screen.ConfigScreenDual;
import org.lwjgl.glfw.GLFW;

import java.util.Iterator;

public class ConfigListWidgetDual extends ConfigListWidget {

    public Integer selectedKeyCode;

    public ConfigListWidgetDual(Minecraft client, int width, int height, int top, int bottom,
                                int itemHeight, Screen parentScreen, Component title) {
        super(client, width, height, top, bottom, itemHeight, parentScreen, title);

        addEntry(new Entry.SettingToggles(width, this));

        addEntry(new ConfigListWidget.Entry.Header(width, client, Component.nullToEmpty(
                "Dual-Hotkey Messages \u2139"), Component.literal("If you opened this " +
                "screen by pressing the config key in-game, you can now send one of the messages " +
                "below by pressing the corresponding key on your keyboard.")));

        addEntry(new Entry.ListHeader(width, client));

        Iterator<Integer> keyIter = QuickMessages.config().getKeyIterDual();
        Iterator<String> valueIter = QuickMessages.config().getValIterDual();

        while (keyIter.hasNext()) {
            addEntry(new Entry.KeyMessageField(width, this, client,
                    keyIter.next(), valueIter.next()));
        }
        addEntry(new Entry.AddMessageButton(width, this));
    }

    public boolean keyPressed(int keyCode, int scanCode) {
        return selectedKeyCode == null;
    }

    public boolean keyReleased(int keyCode, int scanCode) {
        if (!QuickMessages.CONFIG_KEY.matches(keyCode, scanCode)) {
            if (selectedKeyCode == null) {
                if (this.getSelected() == null) {
                    String messages = QuickMessages.config().getMsgDual(keyCode);
                    if (messages != null && client.getConnection() != null &&
                            client.getConnection().isAcceptingMessages()) {
                        String[] messageArr = messages.split(",,");
                        for (String msg : messageArr) {
                            client.setScreen(new ChatScreen(""));
                            if (client.screen instanceof ChatScreen cs) {
                                cs.handleChatInput(msg, QuickMessages.config().addToHistory);
                            }
                            if (QuickMessages.config().showHudMessage) {
                                client.gui.setOverlayMessage(Component.literal(msg)
                                        .setStyle(Style.EMPTY.withColor(12369084)), false);
                            }
                        }
                        client.setScreen(null);
                    }
                }
            }
            else {
                setKey(selectedKeyCode, keyCode);
            }
        }
        return true; // TODO fix return
    }

    protected void setKey(int keyCode, int newKeyCode) {
        if (QuickMessages.config().setKeyDual(keyCode, newKeyCode)) {
            reloadScreen();
        }
    }

    protected void addMessage() {
        if (QuickMessages.config().addMsgDual()) {
            reloadScreen();
        }
    }

    protected void removeMessage(int key) {
        if (QuickMessages.config().removeMsgDual(key)) {
            reloadScreen();
        }
    }

    public void reloadScreen() {
        ConfigListWidget listWidget = new ConfigListWidgetDual(
                client, width, height, top, bottom, itemHeight, parentScreen, screenTitle);
        listWidget.setScrollAmount(this.getScrollAmount());
        client.setScreen(new ConfigScreenDual(parentScreen, client.options, screenTitle, listWidget));
    }

    public ConfigListWidgetDual resize(int width, int height, int top, int bottom) {
        ConfigListWidgetDual listWidget = new ConfigListWidgetDual(
                client, width, height, top, bottom, itemHeight, parentScreen, screenTitle);
        listWidget.setScrollAmount(getScrollAmount());
        return listWidget;
    }

    private void openMonoConfigScreen() {
        ConfigListWidget listWidget = new ConfigListWidgetMono(
                client, width, height, top, bottom, itemHeight, parentScreen, screenTitle);
        client.setScreen(new ConfigScreenDual(parentScreen, client.options, screenTitle, listWidget));
    }


    public abstract static class Entry extends ConfigListWidget.Entry {

        private static class SettingToggles extends Entry {
            SettingToggles(int width, ConfigListWidgetDual listWidget) {
                options.add(CycleButton.booleanBuilder(Component.nullToEmpty("Yes"),
                                Component.nullToEmpty("No"))
                        .withInitialValue(QuickMessages.config().showHudMessage)
                        .withTooltip((status) -> Tooltip.create(Component.nullToEmpty(
                                "Briefly show the sent message as a pop-up above the hotbar.")))
                        .create(width / 2 - 200, 0, 120, 20,
                                Component.literal("Show HUD Display"),
                                (button, status) -> QuickMessages.config().showHudMessage = status));

                options.add(CycleButton.booleanBuilder(Component.nullToEmpty("Yes"),
                                Component.nullToEmpty("No"))
                        .withInitialValue(QuickMessages.config().addToHistory)
                        .withTooltip((status) -> Tooltip.create(Component.nullToEmpty(
                                "Add sent messages/commands to history.")))
                        .create(width / 2 - 75, 0, 120, 20,
                                Component.literal("Add to History"),
                                (button, status) -> QuickMessages.config().addToHistory = status));
                options.add(Button.builder(Component.literal("Mono-Key Settings"),
                                (button) -> listWidget.openMonoConfigScreen())
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

            KeyMessageField(int width, ConfigListWidgetDual listWidget, Minecraft client,
                            int key, String message) {

                boolean isConfigKey = (QuickMessages.CONFIG_KEY.matches(key, 1)
                        && key != GLFW.GLFW_KEY_UNKNOWN);
                String keyName = InputConstants.getKey(key, 1).getDisplayName().getString();

                MutableComponent label = Component.literal(key == GLFW.GLFW_KEY_UNKNOWN ?
                        "[Click to Bind]" : keyName);

                options.add(
                        Button.builder(isConfigKey ? label.withStyle(ChatFormatting.RED) :
                                        label, (button) -> {
                            listWidget.selectedKeyCode = key;
                            button.setMessage(Component.literal("[Press Key]")
                                    .withStyle(ChatFormatting.RED));
                            button.setTooltip(null);
                        })
                                .tooltip(isConfigKey ? Tooltip.create(Component.nullToEmpty(
                                        keyName + " is used to open the GUI. Click to rebind.")) : null)
                                .size(80, 20)
                                .pos(width / 2 - 200, 0)
                                .build());

                EditBox messageField = new EditBox(client.font, width / 2 - 110, 0, 280, 20,
                        Component.literal("Message"));
                messageField.setMaxLength(1024);
                messageField.setValue(message);
                messageField.setResponder((value) -> QuickMessages.config().setMsgDual(key, value.strip()));

                options.add(messageField);

                options.add(Button.builder(Component.literal("X"),
                                (button) -> listWidget.removeMessage(key))
                        .size(20, 20)
                        .pos(width / 2 + 180, 0)
                        .build());
            }
        }


        private static class AddMessageButton extends Entry {
            public AddMessageButton(int width, ConfigListWidgetDual listWidget) {
                options.add(Button.builder(Component.literal("+"),
                                (button) -> listWidget.addMessage())
                        .size(400, 20)
                        .pos(width / 2 - 200, 0)
                        .build());
            }
        }
    }

}