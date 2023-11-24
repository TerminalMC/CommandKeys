package notryken.quickmessages;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import notryken.quickmessages.config.Config;
import notryken.quickmessages.config.MsgKeyMapping;
import notryken.quickmessages.gui.screen.ConfigScreenDual;

public class QuickMessages {
    public static final KeyMapping CONFIG_KEY = new KeyMapping(
            "key.quickmessages.open_menu", InputConstants.Type.KEYSYM,
            InputConstants.KEY_K, "keygroup.quickmessages.main");

    private static Config CONFIG;

    public static void init() {
        Constants.LOG.info("init()");
        CONFIG = loadConfig();
    }

    public static void onEndTick(Minecraft client) {
        while (CONFIG_KEY.consumeClick()) {
            client.setScreen(new ConfigScreenDual(client.screen, client.options,
                    Component.translatable("screen.quickmessages.title"), null));
        }
    }

    public static void onInput() { // TODO this should go in onEndTick or the other way around
        Minecraft client = Minecraft.getInstance();
        if (client.screen == null) {
            for (MsgKeyMapping msgKey : config().getMsgKeyListMono()) {
                if (msgKey.isBound() && !msgKey.isDuplicate()) {
                    while (msgKey.getKeyMapping().consumeClick()) {

                        String[] messageArr = msgKey.msg.split(",,");
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
        }
    }

    public static Config config() {
        if (CONFIG == null) {
            throw new IllegalStateException("Config not yet available");
        }
        return CONFIG;
    }

    private static Config loadConfig() {
        Constants.LOG.info("loadConfig()");
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
}
