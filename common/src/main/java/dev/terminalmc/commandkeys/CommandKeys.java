/*
 * Copyright 2024 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.terminalmc.commandkeys;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.config.Config;
import dev.terminalmc.commandkeys.config.Macro;
import dev.terminalmc.commandkeys.config.Profile;
import dev.terminalmc.commandkeys.gui.screen.OptionsScreen;
import dev.terminalmc.commandkeys.util.ModLogger;
import dev.terminalmc.commandkeys.util.PlaceholderUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

import static dev.terminalmc.commandkeys.util.Localization.localized;
import static dev.terminalmc.commandkeys.util.Localization.translationKey;

public class CommandKeys {
    public static final String MOD_ID = "commandkeys";
    public static final String MOD_NAME = "CommandKeys";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);
    public static final KeyMapping CONFIG_KEY = new KeyMapping(
            translationKey("key", "main.edit"), InputConstants.Type.KEYSYM,
            InputConstants.KEY_K, translationKey("key", "main"));
    public static final Component PREFIX = Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal(MOD_NAME).withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
            .withStyle(ChatFormatting.GRAY);
    
    public static String lastConnection = "";
    
    private static final List<TickCounter> rateLimiter = new ArrayList<>();
    private static class TickCounter {
        int time = 0;
        int tick() {
            return time++;
        }
    }

    public static void init() {
        Config.getAndSave();
    }

    public static void onEndTick(Minecraft mc) {
        // Open config screen via keybind
        while (CONFIG_KEY.consumeClick()) {
            mc.setScreen(new OptionsScreen(mc.screen, true));
        }
        // Tick ratelimiter
        rateLimiter.removeIf((tc) -> tc.tick() > Config.get().getRatelimitTicks());
        // Tick macros
        if (mc.player != null && mc.level != null && !mc.isPaused()) {
            Config.get().activeProfile().getMacros().forEach(Macro::tick);
        }
    }

    public static void onConfigSaved(Config config) {
        // Cache update event (not currently used)
    }

    public static Profile profile() {
        return Config.get().activeProfile();
    }

    public static Screen getConfigScreen(Screen lastScreen) {
        return new OptionsScreen(lastScreen, inGame());
    }

    public static boolean inGame() {
        LocalPlayer player = Minecraft.getInstance().player;
        return (player != null && player.connection.getConnection().isConnected());
    }
    
    public static boolean inSingleplayer() {
        return Minecraft.getInstance().getSingleplayerServer() != null;
    }
    
    public static boolean canTrigger(InputConstants.Key key) {
        if (
                (!inSingleplayer() || Config.get().ratelimitSp) 
                && rateLimiter.size() >= Config.get().getRatelimitCount()) 
        {
            Minecraft.getInstance().gui.getChat().addMessage(PREFIX.copy().append(
                    localized("message", "sendBlocked",
                            key.getDisplayName().copy().withStyle(ChatFormatting.GRAY),
                            Component.literal(String.valueOf(Config.get().getRatelimitCount()))
                                    .withStyle(ChatFormatting.GRAY), 
                            Component.literal(String.valueOf(Config.get().getRatelimitTicks()))
                                    .withStyle(ChatFormatting.GRAY))
                            .withStyle(ChatFormatting.RED)));
            if (Config.getAndSave().ratelimitStrict) rateLimiter.add(new TickCounter());
            return false;
        }
        rateLimiter.add(new TickCounter());
        return true;
    }

    public static void send(String message, boolean addToHistory, boolean showHudMsg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        message = PlaceholderUtil.replace(message);
        // new ChatScreen("").handleChatInput(message, addToHistory)
        // could be slightly better for compat but costs performance.
        if (message.startsWith("/")) {
            mc.player.connection.sendCommand(message.substring(1));
        } else {
            mc.player.connection.sendChat(message);
        }
        if (addToHistory) mc.gui.getChat().addRecentChat(message);
        if (showHudMsg) mc.gui.setOverlayMessage(Component.literal(message)
                .withStyle(ChatFormatting.GRAY), false);
    }

    public static void type(String message) {
        Minecraft.getInstance().setScreen(new ChatScreen(PlaceholderUtil.replace(message)));
    }
}
