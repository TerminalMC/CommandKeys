package dev.terminalmc.commandkeys.gui.widget.list;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.*;
import dev.terminalmc.commandkeys.mixin.KeyMappingAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;

public abstract class KeybindOptionsList extends OptionsList {
    protected Profile profile;
    protected CommandKey selectedCmdKey;
    protected InputConstants.Key heldKey;
    protected InputConstants.Key sendKey;

    public KeybindOptionsList(Minecraft mc, int width, int height, int y,
                              int itemHeight, int entryWidth, int entryHeight) {
        super(mc, width, height, y, itemHeight, entryWidth, entryHeight);
    }

    @Override
    public boolean keyPressed(InputConstants.Key key) {
        if (selectedCmdKey != null) {
            if (key.getValue() == InputConstants.KEY_ESCAPE) {
                selectedCmdKey.setKey(InputConstants.UNKNOWN);
                selectedCmdKey.setLimitKey(InputConstants.UNKNOWN);
                reload();
            }
            else {
                if (heldKey == null) {
                    heldKey = key;
                }
                else {
                    if (key != heldKey) {
                        selectedCmdKey.setLimitKey(heldKey);
                        selectedCmdKey.setKey(key);
                        reload();
                    }
                    else {
                        return false;
                    }
                }
                return false;
            }
            return true;
        }
        else if (getSelected() == null && !key.equals(((KeyMappingAccessor) CommandKeys.CONFIG_KEY).getKey())) {
            sendKey = key;
        }
        return false;
    }

    @Override
    public boolean keyReleased(InputConstants.Key key) {
        if (selectedCmdKey != null) {
            if (heldKey == key) {
                selectedCmdKey.setKey(key);
                selectedCmdKey.setLimitKey(InputConstants.UNKNOWN);
                reload();
                return true;
            }
        }
        else if (key.equals(sendKey)) {
            if (getSelected() == null) {
                for (CommandKey cmdKey : profile.commandKeyMap.get(sendKey)) {
                    if (cmdKey.conflictStrategy.state.equals(QuadState.State.THREE)) {
                        screen.onClose();
                        minecraft.setScreen(null);
                        boolean standardDelay = cmdKey.spaceTicks != -1;
                        int cumulativeDelay = 0;
                        for (Message msg : cmdKey.getMessages()) {
                            cumulativeDelay += standardDelay ? cmdKey.spaceTicks : msg.delayTicks;
                            if (!msg.string.isBlank()) CommandKeys.queue(cumulativeDelay,
                                    msg.string, profile.addToHistory, profile.showHudMessage);
                        }
                        return true;
                    }
                }
            }
            else {
                sendKey = null;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(InputConstants.Key key) {
        return keyPressed(key);
    }

    @Override
    public boolean mouseReleased(InputConstants.Key key) {
        return keyReleased(key);
    }

    public void openMinecraftControlsScreen() {
        minecraft.setScreen(new KeyBindsScreen(screen, Minecraft.getInstance().options));
    }
}
