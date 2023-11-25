package notryken.commandkeys;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import notryken.commandkeys.config.Config;
import notryken.commandkeys.config.MsgKeyMapping;
import notryken.commandkeys.gui.screen.ConfigScreenDual;

public class CommandKeys {
    public static final KeyMapping CONFIG_KEY = new KeyMapping(
            "key.commandkeys.open_menu", InputConstants.Type.KEYSYM,
            InputConstants.KEY_K, "keygroup.commandkeys.main");

    private static Config CONFIG;

    public static void init() {
        CONFIG = loadConfig();
    }

    public static void onEndTick(Minecraft client) {
        while (CONFIG_KEY.consumeClick()) {
            client.setScreen(new ConfigScreenDual(client.screen, client.options,
                    Component.translatable("screen.commandkeys.title"), null));
        }
        if (client.screen == null) {
            for (MsgKeyMapping msgKey : config().getMsgKeyListMono()) {
                if (msgKey.isBound() && !msgKey.isDuplicate()) {
                    while (msgKey.getKeyMapping().consumeClick()) {
                        String[] messageArr = msgKey.msg.split(",,");
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
}
