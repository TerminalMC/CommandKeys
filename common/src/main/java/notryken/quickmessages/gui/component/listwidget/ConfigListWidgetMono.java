package notryken.quickmessages.gui.component.listwidget;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import notryken.quickmessages.QuickMessages;
import notryken.quickmessages.gui.screen.ConfigScreen;

import java.util.Iterator;

public class ConfigListWidgetMono extends ConfigListWidget {

    public KeyMapping selectedKey;

    public ConfigListWidgetMono(Minecraft client, int width, int height, int top, int bottom,
                                int itemHeight, Screen parentScreen, Component title) {
        super(client, width, height, top, bottom, itemHeight, parentScreen, title);

        addEntry(new Entry.SettingToggles(width));

        addEntry(new Entry.ListHeader(width, this, client));

        Iterator<KeyMapping> keyIter = QuickMessages.config().getKeyIterMono();
        Iterator<String> valueIter = QuickMessages.config().getValIterMono();

        while (keyIter.hasNext()) {
            addEntry(new Entry.KeyMessageField(width, this, client,
                    keyIter.next(), valueIter.next()));
        }
        addEntry(new Entry.AddMessageButton(width, this));
    }

    public void onKey(int keyCode, int scanCode) {
        if (selectedKey != null) {
            setKey(selectedKey, keyCode, scanCode);
        }
    }

    protected void setKey(KeyMapping key, int newKeyCode, int newScanCode) {
        key.setKey(InputConstants.getKey(newKeyCode, newScanCode));
        refreshScreen();
    }

    protected void addMessage() {
        if (QuickMessages.config().addMsgMono()) {
            refreshScreen();
        }
    }

    protected void removeMessage(KeyMapping key) {
        if (QuickMessages.config().removeMsgMono(key)) {
            refreshScreen();
        }
    }

    public void refreshScreen() {
        ConfigListWidget listWidget = new ConfigListWidgetMono(
                client, width, height, top, bottom, itemHeight, parentScreen, screenTitle);
        listWidget.setScrollAmount(this.getScrollAmount());
        client.setScreen(new ConfigScreen(parentScreen, client.options, screenTitle, listWidget));
    }

    private void openDualConfigScreen() {
        ConfigListWidget listWidget = new ConfigListWidgetDual(
                client, width, height, top, bottom, itemHeight, parentScreen, screenTitle);
        client.setScreen(new ConfigScreen(parentScreen, client.options, screenTitle, listWidget));
    }

    public abstract static class Entry extends ConfigListWidget.Entry {

        private static class SettingToggles extends Entry {
            SettingToggles(int width) {
                options.add(CycleButton.booleanBuilder(Component.nullToEmpty("Yes"),
                                Component.nullToEmpty("No"))
                        .withInitialValue(QuickMessages.config().showHudMessage)
                        .withTooltip((status) -> Tooltip.create(Component.nullToEmpty(
                                "Briefly show the sent message as a pop-up above the hotbar.")))
                        .create(width / 2 - 120, 32, 117, 20,
                                Component.literal("Show HUD Display"),
                                (button, status) -> QuickMessages.config().showHudMessage = status));

                options.add(CycleButton.booleanBuilder(Component.nullToEmpty("Yes"),
                                Component.nullToEmpty("No"))
                        .withInitialValue(QuickMessages.config().addToHistory)
                        .withTooltip((status) -> Tooltip.create(Component.nullToEmpty(
                                "Add sent messages/commands to history.")))
                        .create(width / 2 + 3, 32, 117, 20,
                                Component.literal("Add to History"),
                                (button, status) -> QuickMessages.config().addToHistory = status));
            }
        }

        public static class ListHeader extends Entry {
            public ListHeader(int width, ConfigListWidgetMono listWidget, Minecraft client) {
                options.add(new StringWidget(width / 2 - 200, 0, 80, 20,
                        Component.nullToEmpty("Hotkey"), client.font));
                options.add(new StringWidget(width / 2 - 110, 0, 280, 20,
                        Component.nullToEmpty("Message/Command"), client.font).alignLeft());
                options.add(Button.builder(Component.literal("Dual-Key Settings"),
                                (button) -> listWidget.openDualConfigScreen())
                        .pos(width / 2 + 100, 0)
                        .size(100, 20)
                        .build());
            }
        }

        private static class KeyMessageField extends Entry {

            KeyMessageField(int width, ConfigListWidgetMono listWidget, Minecraft client,
                            KeyMapping key, String message) {

                boolean duplicate = false;

                MutableComponent keyName = key.getTranslatedKeyMessage().copy();
                MutableComponent label = keyName;
                MutableComponent duplicateKeys = Component.empty();
                Tooltip tooltip;

                if (!key.isUnbound()) {
                    KeyMapping[] allKeys = client.options.keyMappings;
                    // Duplicate detection
                    for (KeyMapping k : allKeys) {
                        if (k.same(key)) {
                            if (duplicate) {
                                duplicateKeys.append(", ");
                            }
                            duplicate = true;
                            duplicateKeys.append(Component.translatable(k.getName()));
                        }
                    }
                }

                if (duplicate) {
                    label = Component.literal("[ ")
                            .append(keyName.copy().withStyle(ChatFormatting.WHITE))
                            .append(" ]").withStyle(ChatFormatting.RED);
                    tooltip = Tooltip.create(Component.translatable(
                            "controls.keybinds.duplicateKeybinds", duplicateKeys));
                } else {
                    tooltip = null;
                }

                options.add(
                        Button.builder(label, (button) -> {
                            listWidget.selectedKey = key;
                            button.setMessage(Component.literal("> ")
                                    .append(keyName.copy().withStyle(ChatFormatting.WHITE))
                                    .append(" <").withStyle(ChatFormatting.YELLOW));
                        })
                                .tooltip(tooltip)
                                .size(80, 20)
                                .pos(width / 2 - 200, 0)
                                .build());

                EditBox messageField = new EditBox(client.font, width / 2 - 110, 0, 280, 20,
                        Component.literal("Message"));
                messageField.setMaxLength(256);
                messageField.setValue(message);
                messageField.setResponder((value) -> QuickMessages.config().setMsgMono(key, value.strip()));
                options.add(messageField);

                options.add(Button.builder(Component.literal("X"),
                                (button) -> listWidget.removeMessage(key))
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