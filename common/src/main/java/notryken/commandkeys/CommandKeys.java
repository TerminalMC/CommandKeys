package notryken.commandkeys;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import notryken.commandkeys.config.Config;
import notryken.commandkeys.gui.component.listwidget.DualKeySetListWidget;
import notryken.commandkeys.gui.screen.ConfigScreen;
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
        CONFIG = Config.load();
    }

    public static void onEndTick(Minecraft minecraft) {
        // Open config screen
        while (CONFIG_KEY.consumeClick()) {
            minecraft.setScreen(new ConfigScreen(minecraft.screen,
                    Component.translatable("screen.commandkeys.title.default"),
                    new DualKeySetListWidget(minecraft, 0, 0, 0, 0,
                            0, -200, 400, 20, 420)));
        }
    }

    public static Config config() {
        if (CONFIG == null) {
            throw new IllegalStateException("Config not yet available");
        }
        return CONFIG;
    }
}
