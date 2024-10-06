package dev.terminalmc.commandkeys.gui.widget.list;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.*;
import dev.terminalmc.commandkeys.mixin.accessor.KeyMappingAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;

import java.util.Collection;

public abstract class MacroBindList extends OptionList {
    protected Profile profile;
    protected Macro selectedMacro;
    protected InputConstants.Key heldKey;
    protected InputConstants.Key sendKey;

    public MacroBindList(Minecraft mc, int width, int height, int top, int bottom,
                         int itemHeight, int entryWidth, int entryHeight) {
        super(mc, width, height, top, bottom, itemHeight, entryWidth, entryHeight);
    }

    @Override
    public boolean keyPressed(InputConstants.Key key) {
        if (selectedMacro != null) {
            if (key.getValue() == InputConstants.KEY_ESCAPE) {
                selectedMacro.setKey(InputConstants.UNKNOWN);
                selectedMacro.setLimitKey(InputConstants.UNKNOWN);
                reload();
            }
            else {
                if (heldKey == null) {
                    heldKey = key;
                }
                else {
                    if (key != heldKey) {
                        selectedMacro.setLimitKey(heldKey);
                        selectedMacro.setKey(key);
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
        if (selectedMacro != null) {
            if (heldKey == key) {
                selectedMacro.setKey(key);
                selectedMacro.setLimitKey(InputConstants.UNKNOWN);
                reload();
                return true;
            }
        }
        else if (key.equals(sendKey)) {
            if (getSelected() == null && CommandKeys.inGame()) {
                Collection<Macro> macros = profile.keyMacroMap.get(sendKey);
                if (!macros.isEmpty()) {
                    screen.onClose();
                    minecraft.setScreen(null);
                    macros.forEach(Macro::trigger);
                    return true;
                }
            }
            sendKey = null;
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
