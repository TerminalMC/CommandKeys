/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package com.notryken.commandkeys;

import com.mojang.blaze3d.platform.InputConstants;
import com.notryken.commandkeys.config.Profile;
import com.notryken.commandkeys.gui.screen.OptionsScreen;
import com.notryken.commandkeys.util.ModLogger;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import com.notryken.commandkeys.config.Config;

import java.net.SocketAddress;

public class CommandKeys {
    public static final String MOD_ID = "commandkeys";
    public static final String MOD_NAME = "CommandKeys";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);
    public static final KeyMapping CONFIG_KEY = new KeyMapping(
            "key.commandkeys.open_config", InputConstants.Type.KEYSYM,
            InputConstants.KEY_K, "keygroup.commandkeys.main");

    private static Config CONFIG;

    public static void init() {
        CONFIG = Config.load();
    }

    public static void onEndTick(Minecraft minecraft) {
        // Open config screen
        while (CONFIG_KEY.consumeClick()) {
            minecraft.setScreen(new OptionsScreen(minecraft.screen, true));
        }
    }

    public static Config config() {
        if (CONFIG == null) {
            throw new IllegalStateException("Config not yet available");
        }
        return CONFIG;
    }

    public static Profile profile() {
        return config().getActiveProfile();
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
