package notryken.commandkeys;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import notryken.commandkeys.config.Config;
import notryken.commandkeys.config.MsgKeyMapping;
import notryken.commandkeys.gui.screen.ConfigScreenDual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandKeys {
    // Constants
    public static final String MOD_ID = "commandkeys";
    public static final String MOD_NAME = "CommandKeys";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);
    public static final KeyMapping CONFIG_KEY = new KeyMapping(
            "key.commandkeys.open_menu", InputConstants.Type.KEYSYM,
            InputConstants.KEY_K, "keygroup.commandkeys.main");

    private static Config CONFIG;

    public static void init() {
        CONFIG = loadConfig();
    }

    public static void onEndTick(Minecraft minecraft) {
        // Open config screen
        while (CONFIG_KEY.consumeClick()) {
            minecraft.setScreen(new ConfigScreenDual(minecraft.screen, minecraft.options,
                    Component.translatable("screen.commandkeys.title"), null));
        }
        // Send messages
        if (minecraft.screen == null) {
            for (MsgKeyMapping msgKey : config().getMsgKeyListMono()) {
                if (msgKey.isBound() && !msgKey.isDuplicate()) {
                    while (msgKey.getKeyMapping().consumeClick()) {
                        String[] messageArr = msgKey.msg.split(",,");
                        for (String msg : messageArr) {
                            if (msg.startsWith("/")) {
                                minecraft.player.connection.sendCommand(msg.substring(1));
                            } else {
                                minecraft.player.connection.sendChat(msg);
                            }
                            if (CommandKeys.config().addToHistory) {
                                minecraft.gui.getChat().addRecentChat(msg);
                            }
                            if (CommandKeys.config().showHudMessage) {
                                minecraft.gui.setOverlayMessage(Component.literal(msg)
                                        .setStyle(Style.EMPTY.withColor(12369084)), false);
                            }
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
            CommandKeys.LOG.error("Failed to load configuration file", e);
            CommandKeys.LOG.error("Using default configuration file");
            Config newConfig = new Config();
            newConfig.writeChanges();
            return newConfig;
        }
    }
}
