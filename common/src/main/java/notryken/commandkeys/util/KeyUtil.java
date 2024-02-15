package notryken.commandkeys.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import notryken.commandkeys.config.CommandKey;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static notryken.commandkeys.CommandKeys.profile;

public class KeyUtil {

    public static boolean handleKey(InputConstants.Key key) {
        boolean cancelClick = false;
        boolean cancelNext = false;
        if (Minecraft.getInstance().screen == null && CommandKey.MAP.containsKey(key)) {

            CommandKey cmdKey = null;
            Set<CommandKey> commandKeys = CommandKey.MAP.get(key);
            for (CommandKey ck1 : commandKeys) {
                if (ck1.getLimitKey().equals(InputConstants.UNKNOWN)) {
                    // Found a matching CommandKey, but preference the ones
                    // with modifier keys that are down.
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
//                    case ZERO -> send = getConflictKeyMapping(key) == null;
                    case ZERO -> send = !KeyMapping.MAP.containsKey(key);
                    case TWO -> override = true;
                }

                if (send) {
                    cancelNext = true;
                    cancelClick = !cmdKey.fullSend || override;

                    if (cmdKey.cycle) {
                        // Strategy to allow spacer blank messages
                        String messages = cmdKey.messages.get(cmdKey.nextIndex);
                        if (messages != null && !messages.isBlank()) {
                            if (cmdKey.fullSend) {
                                for (String msg : messages.split(",,")) {
                                    if (!msg.isBlank()) {
                                        KeyUtil.send(msg, profile().addToHistory, profile().showHudMessage);
                                    }
                                }
                            }
                            else {
                                KeyUtil.type(messages);
                            }
                        }
                        if (cmdKey.nextIndex < cmdKey.messages.size() - 1) {
                            cmdKey.nextIndex ++;
                        }
                        else {
                            cmdKey.nextIndex = 0;
                        }
                    }
                    else {
                        if (cmdKey.fullSend) {
                            for (String msg : cmdKey.messages) {
                                KeyUtil.send(msg, profile().addToHistory, profile().showHudMessage);
                            }
                        }
                        else if (!cmdKey.messages.isEmpty()) {
                            KeyUtil.type(cmdKey.messages.get(0));
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

    public static @Nullable KeyMapping getConflictKeyMapping(InputConstants.Key key) {
        for (KeyMapping mcKeyMapping : Minecraft.getInstance().options.keyMappings) {
            if (mcKeyMapping.key.equals(key)) {
                return mcKeyMapping;
            }
        }
        return null;
    }
}
