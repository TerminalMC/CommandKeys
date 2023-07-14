package notryken.quickmessages.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static notryken.quickmessages.client.QuickMessagesClient.config;

public class ConfigListWidget extends ElementListWidget<ConfigListWidget.Entry>
{
    public final MinecraftClient client;
    public final Screen parent;
    public final Text title;

    public int selectedKey = -1;

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

        Iterator<Integer> keyIter = config.getKeyIter();
        Iterator<String> valueIter = config.getValueIter();

        while (keyIter.hasNext()) {
            this.addEntry(new Entry.KeyMessageField(width, this, client,
                    keyIter.next(), valueIter.next()));
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

    public void pressedKey(int keyCode)
    {
        if (selectedKey == -1) {
            if (this.getSelectedOrNull() == null &&
                    client.getNetworkHandler() != null &&
                    client.getNetworkHandler().isConnectionOpen())
            {
                String message = config.getMessage(keyCode);
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
            config.setKey(selectedKey, keyCode);
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
                this.options.add(new TextWidget(width / 2 - 200, 0, 80, 20,
                        Text.of("Hotkey"), client.textRenderer));
                this.options.add(new TextWidget(width / 2 - 110, 0, 280, 20,
                        Text.of("Message/Command"), client.textRenderer));
            }
        }

        private static class KeyMessageField extends Entry {
            private final int key;

            KeyMessageField(int width, ConfigListWidget listWidget,
                            MinecraftClient client, int key, String message)
            {
                super(listWidget);
                this.key = key;

                String label = (key == Integer.MAX_VALUE ? "[Click to Bind]" :
                        InputUtil.fromKeyCode(key, key).getLocalizedText()
                                .getString());

                options.add(
                        ButtonWidget.builder(Text.of(label), (button) -> {
                            listWidget.selectedKey = key;
                            button.setMessage(Text.literal("[Press Key]")
                                    .setStyle(Style.EMPTY.withColor(16267834)));
                        })
                                .size(80, 20)
                                .position(width / 2 - 200, 0)
                                .build());

                TextFieldWidget messageField = new TextFieldWidget(
                        client.textRenderer, width / 2 - 110, 0, 280, 20,
                        Text.literal("Message"));
                messageField.setMaxLength(256);
                messageField.setText(message);
                messageField.setChangedListener(this::setMessage);

                options.add(messageField);

                options.add(ButtonWidget.builder(Text.literal("X"),
                                (button) -> {
                                    config.removeMessage(key);
                                    listWidget.refreshScreen();
                })
                        .size(20, 20)
                        .position(width / 2 + 180, 0)
                        .build());
            }

            private void setMessage(String message)
            {
                config.setMessage(this.key, message.strip());
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