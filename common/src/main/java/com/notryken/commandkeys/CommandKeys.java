package com.notryken.commandkeys;

import com.mojang.blaze3d.platform.InputConstants;
import com.notryken.commandkeys.config.Profile;
import com.notryken.commandkeys.gui.screen.ConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import com.notryken.commandkeys.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

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
            minecraft.setScreen(new ConfigScreen(minecraft.screen, true));
        }
    }

    public static Config config() {
        if (CONFIG == null) {
            throw new IllegalStateException("Config not yet available");
        }
        return CONFIG;
    }

    public static Profile profile() {
        if (CONFIG == null) {
            throw new IllegalStateException("Config not yet available");
        }
        return CONFIG.getActiveProfile();
    }

    public static Screen getConfigScreen(Screen lastScreen) {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean inGame = (player != null && player.connection.getConnection().isConnected());
        return new ConfigScreen(lastScreen, inGame);
    }

    public static SocketAddress activeAddress() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.connection.getConnection().isConnected()) {
            return player.connection.getConnection().getRemoteAddress();
        }
        return null;
    }
}
