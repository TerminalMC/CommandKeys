package notryken.quickmessages.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import notryken.quickmessages.client.QuickMessagesClient;

import java.util.ArrayList;
import java.util.List;

import static notryken.quickmessages.client.QuickMessagesClient.config;

public class ConfigListWidget extends ElementListWidget<ConfigListWidget.Entry>
{
    public final MinecraftClient client;
    public final Screen parent;
    public final Text title;

    public String selectedKey;

    public ConfigListWidget(MinecraftClient client, int width, int height,
                            int top, int bottom, int itemHeight,
                            Screen parent, Text title)
    {
        super(client, width, height, top, bottom, itemHeight);
        this.setRenderSelection(true);

        this.client = client;
        this.parent = parent;
        this.title = title;

        this.addEntry(new Entry.Header(width, this, client, Text.of("Press " +
                "the Hotkey of the Message you want to Send, or Edit below.")));
        this.addEntry(new Entry.ListHeader(width, this, client));

        int index = 0;
        for (String[] keyMessage : config.getKeyMessages()) {

            this.addEntry(new Entry.KeyMessageField(width, this, client,
                    keyMessage[0], keyMessage[1], index));
            index++;
        }
        this.addEntry(new Entry.AddMessageButton(width, this));
    }

    protected void refreshScreen()
    {
        client.setScreen(new ConfigScreen(parent));
    }

    public int getRowWidth()
    {
        return 400;
    }

    protected int getScrollbarPositionX()
    {
        return super.getScrollbarPositionX() + 82;
    }

    public void pressedKey(String key)
    {
        if (selectedKey == null) {
            if (this.getSelectedOrNull() == null &&
                    client.getNetworkHandler() != null &&
                    client.getNetworkHandler().isConnectionOpen())
            {
                String message = QuickMessagesClient.config.getMessage(key);
                if (message != null) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.setScreen(new ChatScreen(message));
                    if (client.currentScreen instanceof ChatScreen cs) {
                        cs.sendMessage(message, true);
                    }
                    client.setScreen(null);
                    client.inGameHud.setOverlayMessage(Text.literal(
                            message).setStyle(Style.EMPTY.withColor(12369084)),
                            false);
                }
            }
        }
        else {
            config.setKey(selectedKey, key);
            refreshScreen();
        }
    }

    public static class Entry extends
            ElementListWidget.Entry<ConfigListWidget.Entry>
    {
        public final List<ClickableWidget> options;
        public final ConfigListWidget listWidget;

        public Entry(ConfigListWidget listWidget)
        {
            this.options = new ArrayList<>();
            this.listWidget = listWidget;
        }

        public void render(DrawContext context, int index, int y, int x,
                           int entryWidth, int entryHeight,
                           int mouseX, int mouseY,
                           boolean hovered, float tickDelta)
        {
            this.options.forEach((button) -> {
                button.setY(y);
                button.render(context, mouseX, mouseY, tickDelta);
            });
        }

        public List<? extends Element> children()
        {
            return this.options;
        }

        public List<? extends Selectable> selectableChildren()
        {
            return this.options;
        }

        public static class Header extends Entry
        {
            public Header(int width, ConfigListWidget listWidget,
                          MinecraftClient client, Text label)
            {
                super(listWidget);
                this.options.add(new TextWidget(width / 2 - 120, 0, 240, 20,
                        label, client.textRenderer));
            }
        }

        public static class ListHeader extends Entry
        {
            public ListHeader(int width, ConfigListWidget listWidget,
                          MinecraftClient client)
            {
                super(listWidget);
                this.options.add(new TextWidget(width / 2 - 200, 0, 100, 20,
                        Text.of("Hotkey"), client.textRenderer));
                this.options.add(new TextWidget(width / 2 - 90, 0, 260, 20,
                        Text.of("Message/Command"), client.textRenderer));
            }
        }

        private static class KeyMessageField extends Entry {
            private final String key;

            KeyMessageField(int width, ConfigListWidget listWidget,
                            MinecraftClient client, String key, String message,
                            int index)
            {
                super(listWidget);
                this.key = key;

                options.add(
                        ButtonWidget.builder(Text.translatable(key),
                                        (button) -> {
                            listWidget.selectedKey = key;
                            button.setMessage(Text.of("Press Key"));
                        })
                                .size(100, 20)
                                .position(width / 2 - 200, 0)
                                .build());

                TextFieldWidget messageField = new TextFieldWidget(
                        client.textRenderer, width / 2 - 90, 0, 260, 20,
                        Text.literal("Message"));
                messageField.setMaxLength(256);
                messageField.setText(message);
                messageField.setChangedListener(this::setMessage);

                options.add(messageField);

                options.add(ButtonWidget.builder(Text.literal("X"),
                                (button) -> {
                                    config.removeMessage(index);
                                    listWidget.refreshScreen();
                })
                        .size(20, 20)
                        .position(width / 2 + 180, 0)
                        .build());
            }

            private void setMessage(String message)
            {
                config.setMessage(this.key, message);
            }
        }


        private static class AddMessageButton extends Entry
        {
            public AddMessageButton(int width, ConfigListWidget listWidget)
            {
                super(listWidget);

                options.add(ButtonWidget.builder(Text.literal("+"),
                                (button) -> {
                                    config.addMessage();
                                    listWidget.refreshScreen();
                                })
                        .size(400, 20)
                        .position(width / 2 - 200, 0)
                        .build());
            }
        }
    }

}