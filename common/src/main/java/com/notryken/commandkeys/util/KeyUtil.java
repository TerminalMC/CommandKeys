package com.notryken.commandkeys.util;

import com.mojang.blaze3d.platform.InputConstants;
import com.notryken.commandkeys.config.CommandKey;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

import java.util.Set;

import static com.notryken.commandkeys.CommandKeys.profile;

public class KeyUtil {

    public static boolean handleKey(InputConstants.Key key) {
        boolean cancelClick = false;
        boolean cancelNext = false;
        if (Minecraft.getInstance().screen == null && CommandKey.MAP.containsKey(key)) {

            CommandKey cmdKey = null;
            Set<CommandKey> commandKeys = CommandKey.MAP.get(key);
            for (CommandKey ck1 : commandKeys) {
                if (ck1.getLimitKey().equals(InputConstants.UNKNOWN)) {
                    // Found a matching single-key CommandKey, but preference
                    // the ones with modifier keys that are down.
                    cmdKey = ck1;
                    for (CommandKey ck2 : commandKeys) {
                        if (!ck2.getLimitKey().equals(InputConstants.UNKNOWN) &&
                                InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(),
                                        ck2.getLimitKey().getValue())) {
                            cmdKey = ck2;
                            break;
                        }
                    }
                    break;
                }
                else if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(),
                        ck1.getLimitKey().getValue())) {
                    cmdKey = ck1;
                    break;
                }
            }
            
            if (cmdKey != null) {
                
                boolean send = true;
                boolean override = false;
                switch(cmdKey.conflictStrategy.state) {
                    // Can't use MAP.contains(key) because Forge replaces the
                    // java.util.Map with a KeyMappingLookup thing.
                    case ZERO -> send = KeyMapping.MAP.get(key) == null;
                    case TWO -> override = true;
                }

                if (send) {
                    cancelNext = true;
                    cancelClick = override;

                    switch(cmdKey.sendStrategy.state) {
                        case ZERO -> {
                            for (String msg : cmdKey.messages) {
                                KeyUtil.send(msg, profile().addToHistory, profile().showHudMessage);
                            }
                        }
                        case ONE -> {
                            if (!cmdKey.messages.isEmpty()) {
                                cancelClick = true;
                                KeyUtil.type(cmdKey.messages.get(0));
                            }
                        }
                        case TWO -> {
                            // Strategy to allow spacer blank messages, and multiple
                            // messages per cycling key-press.
                            String messages = cmdKey.messages.get(cmdKey.cycleIndex);
                            if (messages != null && !messages.isBlank()) {
                                for (String msg : messages.split(",,")) {
                                    if (!msg.isBlank()) {
                                        KeyUtil.send(msg, profile().addToHistory, profile().showHudMessage);
                                    }
                                }
                            }
                            if (cmdKey.cycleIndex < cmdKey.messages.size() - 1) {
                                cmdKey.cycleIndex ++;
                            }
                            else {
                                cmdKey.cycleIndex = 0;
                            }
                        }
                    }
                }
            }
        }
        if (!cancelClick) KeyMapping.click(key);
        return cancelNext;
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

    public static void type(String message) {
        Minecraft.getInstance().setScreen(new ChatScreen(message));
    }
}
