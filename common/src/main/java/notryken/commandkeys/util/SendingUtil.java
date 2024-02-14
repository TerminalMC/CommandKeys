package notryken.commandkeys.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import notryken.commandkeys.config.CommandMonoKey;

import static notryken.commandkeys.CommandKeys.config;

public class SendingUtil {

    public static boolean handleKey(InputConstants.Key key) {
        boolean cancelClick = false;
        boolean cancelNext = false;
        if (Minecraft.getInstance().screen == null) {
            CommandMonoKey commandKey = CommandMonoKey.MAP.get(key);
            if (commandKey != null) {
                boolean send = switch(commandKey.onlyIfKey.state) {
                    case ZERO -> true;
                    case ONE -> Screen.hasControlDown();
                    case TWO -> Screen.hasAltDown();
                    case THREE -> Screen.hasShiftDown();
                };

                boolean override = false;
                if (send) {
                    switch(commandKey.conflictStrategy.state) {
                        case ZERO -> send = KeyMapping.MAP.get(key) == null;
                        case TWO -> override = true;
                    }
                }

                if (send) {
                    cancelNext = true;
                    cancelClick = !commandKey.fullSend || override;

                    if (commandKey.cycle) {
                        if (commandKey.nextIndex < commandKey.messages.size() - 1) {
                            // Strategy to allow spacer blank messages
                            String msg = commandKey.messages.get(commandKey.nextIndex);
                            if (!msg.isBlank()) {
                                if (commandKey.fullSend) {
                                    SendingUtil.send(msg, config().monoAddToHistory, config().monoShowHudMessage);
                                }
                                else {
                                    SendingUtil.type(msg);
                                }
                            }
                            commandKey.nextIndex ++;
                        }
                        else if (commandKey.nextIndex < commandKey.messages.size()) {
                            if (commandKey.fullSend) {
                                SendingUtil.send(commandKey.messages.get(commandKey.nextIndex),
                                        config().monoAddToHistory, config().monoShowHudMessage);
                            }
                            else {
                                SendingUtil.type(commandKey.messages.get(commandKey.nextIndex));
                            }
                            commandKey.nextIndex = 0;
                        }
                        else {
                            // no messages or index is out of range, reset
                            commandKey.nextIndex = 0;
                        }
                    }
                    else {
                        if (commandKey.fullSend) {
                            for (String msg : commandKey.messages) {
                                SendingUtil.send(msg, config().monoAddToHistory, config().monoShowHudMessage);
                            }
                        }
                        else if (!commandKey.messages.isEmpty()) {
                            SendingUtil.type(commandKey.messages.get(0));
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
