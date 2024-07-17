package dev.terminalmc.commandkeys;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.config.Config;
import dev.terminalmc.commandkeys.config.Profile;
import dev.terminalmc.commandkeys.gui.screen.OptionsScreen;
import dev.terminalmc.commandkeys.util.ModLogger;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static dev.terminalmc.commandkeys.util.Localization.translationKey;

public class CommandKeys {
    public static final String MOD_ID = "commandkeys";
    public static final String MOD_NAME = "CommandKeys";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);
    public static final KeyMapping CONFIG_KEY = new KeyMapping(
            translationKey("key", "open_config"), InputConstants.Type.KEYSYM,
            InputConstants.KEY_K, translationKey("key_group"));

    public static String lastConnection = "";

    public static List<QueuedMessage> queuedMessages = new ArrayList<>();

    public static void init() {
        Config.getAndSave();
    }

    public static void onEndTick(Minecraft minecraft) {
        // Open config screen
        while (CONFIG_KEY.consumeClick()) {
            minecraft.setScreen(new OptionsScreen(minecraft.screen, true));
        }

        // Tick queued commands
        Iterator<QueuedMessage> iter = queuedMessages.iterator();
        while (iter.hasNext()) {
            QueuedMessage qm = iter.next();
            if (qm.tick()) {
                send(qm.message, qm.addToHistory, qm.showHudMsg);
                iter.remove();
            }
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

    public static void send(String message, boolean addToHistory, boolean showHudMsg) {
        Minecraft minecraft = Minecraft.getInstance();
        if (message.startsWith("/")) {
            minecraft.player.connection.sendCommand(message.substring(1));
        } else {
            minecraft.player.connection.sendChat(message);
        }
        if (addToHistory) {
            minecraft.gui.getChat().addRecentChat(message);
        }
        if (showHudMsg) {
            minecraft.gui.setOverlayMessage(Component.literal(message)
                    .withStyle(ChatFormatting.GRAY), false);
        }
    }

    public static void queue(int ticks, String message, boolean addToHistory, boolean showHudMsg) {
        queuedMessages.add(new QueuedMessage(ticks, message, addToHistory, showHudMsg));
    }

    public static void type(String message) {
        Minecraft.getInstance().setScreen(new ChatScreen(message));
    }

    public static class QueuedMessage {
        int ticks;
        String message;
        boolean addToHistory;
        boolean showHudMsg;

        public QueuedMessage(int ticks, String message, boolean addToHistory, boolean showHudMsg) {
            this.ticks = ticks;
            this.message = message;
            this.addToHistory = addToHistory;
            this.showHudMsg = showHudMsg;
        }

        public boolean tick() {
            return ticks-- <= 0;
        }
    }
}
