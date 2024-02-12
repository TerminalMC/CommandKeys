package notryken.commandkeys.gui.component.listwidget;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import notryken.commandkeys.CommandKeys;
import notryken.commandkeys.gui.screen.ConfigScreenDual;
import org.lwjgl.glfw.GLFW;

import java.util.Iterator;

public class ConfigListWidgetDual extends ConfigListWidget {

    public Integer selectedKeyCode;

    public ConfigListWidgetDual(Minecraft client, int width, int height, int top, int bottom,
                                int itemHeight, Screen parentScreen, Component title) {
        super(client, width, height, top, bottom, itemHeight, parentScreen, title);

        addEntry(new Entry.SettingToggles(width, this));

        addEntry(new ConfigListWidget.Entry.Header(width, client, Component.nullToEmpty(
                "--------------------- Dual-Hotkey Messages \u2139 ---------------------"),
                Component.literal("If you opened this screen by pressing the config key in-game, " +
                        "you can now send one of the messages below by pressing the corresponding " +
                        "key on your keyboard.")));

        addEntry(new Entry.ListHeader(width, client));

        Iterator<Integer> keyIter = CommandKeys.config().getKeyIterDual();
        Iterator<String> valueIter = CommandKeys.config().getValIterDual();

        while (keyIter.hasNext()) {
            addEntry(new Entry.KeyMessageField(width, this, client,
                    keyIter.next(), valueIter.next()));
        }
        addEntry(new Entry.AddMessageButton(width, this));
    }

    @Override
    public boolean keyPressed(InputConstants.Key key) {
        return selectedKeyCode != null;
    }

    @Override
    public boolean handleKey(InputConstants.Key key) {
        if (key.getValue() == InputConstants.KEY_ESCAPE || key.equals(InputConstants.UNKNOWN)) {
            reloadScreen();
            return false;
        }
        else if (key.equals(CommandKeys.CONFIG_KEY.key) ||
                key.getType().equals(InputConstants.Type.MOUSE)) {
            return false;
        }
        else {
            int keyCode = key.getValue();
            if (selectedKeyCode == null) {
                if (getSelected() == null) {
                    String messages = CommandKeys.config().getMsgDual(keyCode);
                    if (messages != null && client.getConnection() != null &&
                            client.getConnection().isAcceptingMessages()) {
                        String[] messageArr = messages.split(",,");
                        for (String msg : messageArr) {
                            client.setScreen(new ChatScreen(""));
                            if (client.screen instanceof ChatScreen cs) {
                                cs.handleChatInput(msg, CommandKeys.config().addToHistory);
                            }
                            if (CommandKeys.config().showHudMessage) {
                                client.gui.setOverlayMessage(Component.literal(msg)
                                        .setStyle(Style.EMPTY.withColor(12369084)), false);
                            }
                        }
                        client.setScreen(null);
                    }
                }
            }
            else {
                CommandKeys.config().setKeyDual(selectedKeyCode, keyCode);
                reloadScreen();

            }
            return true;
        }
    }

    protected void addMessage() {
        if (CommandKeys.config().addMsgDual()) {
            reloadScreen();
        }
    }

    protected void removeMessage(int key) {
        if (CommandKeys.config().removeMsgDual(key)) {
            reloadScreen();
        }
    }

    @Override
    public void reloadScreen() {
        ConfigListWidget listWidget = new ConfigListWidgetDual(
                client, width, height, top, bottom, itemHeight, parentScreen, screenTitle);
        listWidget.setScrollAmount(this.getScrollAmount());
        client.setScreen(new ConfigScreenDual(parentScreen, client.options, screenTitle, listWidget));
    }

    @Override
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
                options.add(Button.builder(Component.literal("Mono-Key Options"),
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

                boolean isConfigKey = (CommandKeys.CONFIG_KEY.matches(key, 1)
                        && key != GLFW.GLFW_KEY_UNKNOWN);
                String keyName = InputConstants.getKey(key, 1).getDisplayName().getString();

                MutableComponent label = Component.literal(key == GLFW.GLFW_KEY_UNKNOWN ?
                        "Not Bound" : keyName);

                options.add(
                        Button.builder(isConfigKey ? Component.literal("[ ")
                                .append(label.withStyle(ChatFormatting.WHITE))
                                .append(" ]").withStyle(ChatFormatting.RED) :
                                        label, (button) -> {
                            listWidget.selectedKeyCode = key;
                            button.setMessage(Component.literal("> ")
                                    .append(label.copy().withStyle(ChatFormatting.WHITE)
                                            .withStyle(ChatFormatting.UNDERLINE))
                                    .append(" <").withStyle(ChatFormatting.YELLOW));
                            button.setTooltip(null);
                        })
                                .tooltip(isConfigKey ? Tooltip.create(Component.nullToEmpty(
                                        keyName + " is already used to open this screen. Click to rebind.")) : null)
                                .size(80, 20)
                                .pos(width / 2 - 200, 0)
                                .build());

                EditBox messageField = new EditBox(client.font, width / 2 - 110, 0, 280, 20,
                        Component.literal("Message"));
                messageField.setMaxLength(1024);
                messageField.setValue(message);
                messageField.setResponder((value) -> CommandKeys.config().setMsgDual(key, value.strip()));

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