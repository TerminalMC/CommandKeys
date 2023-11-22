package notryken.quickmessages;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import notryken.quickmessages.config.Config;
import notryken.quickmessages.gui.screen.ConfigScreen;

import java.util.Iterator;

public class QuickMessages {
    public static final KeyMapping CONFIG_KEY = new KeyMapping(
            "key.quickmessages.open_menu", InputConstants.Type.KEYSYM,
            InputConstants.KEY_K, "keygroup.quickmessages.title");

    private static Config CONFIG;

    public static void init() {
        CONFIG = loadConfig();
    }

    public static void onEndTick(Minecraft client) {
        while (CONFIG_KEY.consumeClick()) {
            client.setScreen(getScreenDual());
        }
        if (client.screen == null) {
            Iterator<KeyMapping> iter = config().getKeyIterMono();
            while (iter.hasNext()) {
                KeyMapping key = iter.next();
                while (key.consumeClick()) {
                    String msg = config().getMsgMono(key);
                    if (msg != null) {
                        client.setScreen(new ChatScreen(msg));
                        if (client.screen instanceof ChatScreen cs) {
                            cs.handleChatInput(msg, QuickMessages.config().addToHistory);
                        }
                        client.setScreen(null);
                        if (QuickMessages.config().showHudMessage) {
                            Constants.LOG.info("the thing is true");
                            client.gui.setOverlayMessage(Component.literal(msg)
                                    .setStyle(Style.EMPTY.withColor(12369084)), false);
                        }
                    }
                }
            }
        }
    }

    public static Config config() {
        if (CONFIG == null) {
            throw new IllegalStateException("Config not yet available");
        }
        return CONFIG;
    }

    private static Config loadConfig() {
        try {
            return Config.load();
        } catch (Exception e) {
            Constants.LOG.error("Failed to load configuration file", e);
            Constants.LOG.error("Using default configuration file");
            Config newConfig = new Config();
            newConfig.writeChanges();
            return newConfig;
        }
    }

    public static Screen getScreenMono() {
        Minecraft client = Minecraft.getInstance();
        Component title = Component.literal("Quick Messages");
        return new ConfigScreen(client.screen, client.options, title, 0);
    }

    public static Screen getScreenDual() {
        Minecraft client = Minecraft.getInstance();
        Component title = Component.literal("Quick Messages");
        return new ConfigScreen(client.screen, client.options, title, 1);
    }
}
