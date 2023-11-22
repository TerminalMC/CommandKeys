package notryken.quickmessages.gui.component.listwidget;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import notryken.quickmessages.QuickMessages;
import notryken.quickmessages.gui.screen.ConfigScreen;
import org.lwjgl.glfw.GLFW;

import java.util.Iterator;

public class ConfigListWidgetDual extends ConfigListWidget {

    public Integer selectedKey;

    public ConfigListWidgetDual(Minecraft client, int width, int height, int top, int bottom,
                                int itemHeight, Screen parentScreen, Component title) {
        super(client, width, height, top, bottom, itemHeight, parentScreen, title);

        addEntry(new ConfigListWidget.Entry.Header(width, client, Component.nullToEmpty(
                "Press the Hotkey of a message to Send, or Edit below."), null));
        addEntry(new Entry.ListHeader(width, this, client));

        Iterator<Integer> keyIter = QuickMessages.config().getKeyIterDual();
        Iterator<String> valueIter = QuickMessages.config().getValIterDual();

        while (keyIter.hasNext()) {
            addEntry(new Entry.KeyMessageField(width, this, client,
                    keyIter.next(), valueIter.next()));
        }
        addEntry(new Entry.AddMessageButton(width, this));
    }

    public void onKey(int keyCode, int scanCode) {
        if (!QuickMessages.CONFIG_KEY.matches(keyCode, scanCode)) {
            if (selectedKey == null) {
                if (this.getSelected() == null) {
                    String message = QuickMessages.config().getMsgDual(keyCode);
                    if (message != null) {
                        client.setScreen(new ChatScreen(message));
                        if (client.screen instanceof ChatScreen cs) {
                            cs.handleChatInput(message, QuickMessages.config().addToHistory);
                        }
                        client.setScreen(null);
                        if (QuickMessages.config().showHudMessage) {
                            client.gui.setOverlayMessage(Component.literal(message)
                                    .setStyle(Style.EMPTY.withColor(12369084)), false);
                        }
                    }
                }
            }
            else {
                setKey(selectedKey, keyCode);
            }
        }
    }

    protected void setKey(int keyCode, int newKeyCode) {
        if (QuickMessages.config().setKeyDual(keyCode, newKeyCode)) {
            refreshScreen();
        }
    }

    protected void addMessage() {
        if (QuickMessages.config().addMsgDual()) {
            refreshScreen();
        }
    }

    protected void removeMessage(int key) {
        if (QuickMessages.config().removeMsgDual(key)) {
            refreshScreen();
        }
    }

    public void refreshScreen() {
        ConfigListWidget listWidget = new ConfigListWidgetDual(
                client, width, height, top, bottom, itemHeight, parentScreen, screenTitle);
        listWidget.setScrollAmount(this.getScrollAmount());
        client.setScreen(new ConfigScreen(parentScreen, client.options, screenTitle, listWidget));
    }

    private void openMonoConfigScreen() {
        ConfigListWidget listWidget = new ConfigListWidgetMono(
                client, width, height, top, bottom, itemHeight, parentScreen, screenTitle);
        client.setScreen(new ConfigScreen(parentScreen, client.options, screenTitle, listWidget));
    }


    public abstract static class Entry extends ConfigListWidget.Entry {

        public static class ListHeader extends Entry {
            public ListHeader(int width, ConfigListWidgetDual listWidget, Minecraft client) {
                options.add(new StringWidget(width / 2 - 200, 0, 80, 20,
                        Component.nullToEmpty("Hotkey"), client.font));
                options.add(new StringWidget(width / 2 - 110, 0, 280, 20,
                        Component.nullToEmpty("Message/Command"), client.font).alignLeft());
                options.add(Button.builder(Component.literal("More Settings"),
                                (button) -> listWidget.openMonoConfigScreen())
                        .pos(width / 2 + 100, 0)
                        .size(100, 20)
                        .build());
            }
        }

        private static class KeyMessageField extends Entry {

            KeyMessageField(int width, ConfigListWidgetDual listWidget, Minecraft client,
                            int key, String message) {

                boolean isConfigKey = (QuickMessages.CONFIG_KEY.matches(key, key)
                        && key != GLFW.GLFW_KEY_UNKNOWN);
                String keyName = InputConstants.getKey(key, key).getDisplayName().getString();

                MutableComponent label = Component.literal(key == GLFW.GLFW_KEY_UNKNOWN ?
                        "[Click to Bind]" : keyName);

                options.add(
                        Button.builder(isConfigKey ? label.withStyle(ChatFormatting.RED) :
                                        label, (button) -> {
                            listWidget.selectedKey = key;
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
                messageField.setMaxLength(256);
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