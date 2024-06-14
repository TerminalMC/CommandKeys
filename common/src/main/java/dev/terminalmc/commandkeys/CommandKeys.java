package dev.terminalmc.commandkeys;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.config.Config;
import dev.terminalmc.commandkeys.config.Profile;
import dev.terminalmc.commandkeys.gui.screen.OptionsScreen;
import dev.terminalmc.commandkeys.util.ModLogger;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;

import java.net.SocketAddress;

import static dev.terminalmc.commandkeys.util.Localization.translationKey;

public class CommandKeys {
    public static final String MOD_ID = "commandkeys";
    public static final String MOD_NAME = "CommandKeys";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);
    public static final KeyMapping CONFIG_KEY = new KeyMapping(
            translationKey("key", "open_config"), InputConstants.Type.KEYSYM,
            InputConstants.KEY_K, translationKey("key_group"));

    public static void init() {
        Config.getAndSave();
    }

    public static void onEndTick(Minecraft minecraft) {
        // Open config screen
        while (CONFIG_KEY.consumeClick()) {
            minecraft.setScreen(new OptionsScreen(minecraft.screen, true));
        }
    }

    public static void onConfigSaved(Config config) {
        // If you are maintaining caches based on config values, update them here.
    }

    public static Profile profile() {
        return Config.get().getActiveProfile();
    }

    public static Screen getConfigScreen(Screen lastScreen) {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean inGame = (player != null && player.connection.getConnection().isConnected());
        return new OptionsScreen(lastScreen, inGame);
    }

    public static SocketAddress activeAddress() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.connection.getConnection().isConnected()) {
            return player.connection.getConnection().getRemoteAddress();
        }
        return null;
    }
}
