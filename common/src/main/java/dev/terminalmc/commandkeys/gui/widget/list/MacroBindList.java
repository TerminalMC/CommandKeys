/*
 * Copyright 2025 TerminalMC
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

package dev.terminalmc.commandkeys.gui.widget.list;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.*;
import dev.terminalmc.commandkeys.mixin.accessor.KeyMappingAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * An extension of {@link OptionList} allowing handling of key presses and 
 * mouse button clicks for setting keybinds and triggering macros.
 */
public abstract class MacroBindList extends OptionList {
    protected @NotNull Profile profile;
    private @Nullable Macro macro;
    private @Nullable Keybind keybind;
    private InputConstants.Key heldKey;
    private InputConstants.Key sendKey;

    public MacroBindList(Minecraft mc, int width, int height, int y,
                         int itemHeight, int entryWidth, int entryHeight, 
                         @NotNull Profile profile) {
        super(mc, width, height, y, itemHeight, entryWidth, entryHeight);
        this.profile = profile;
    }
    
    protected void setSelected(@NotNull Macro macro, @NotNull Keybind keybind) {
        if (!profile.getMacros().contains(macro)) throw new IllegalArgumentException(
                "Specified macro does not exist in profile.");
        if (!macro.usesKeybind(keybind)) throw new IllegalArgumentException(
                "Specified keybind not used by specified macro.");
        this.macro = macro;
        this.keybind = keybind;
    }

    @Override
    public boolean keyPressed(InputConstants.Key key) {
        if (macro != null && keybind != null) {
            if (key.getValue() == InputConstants.KEY_ESCAPE) {
                profile.setKey(macro, keybind, InputConstants.UNKNOWN);
                profile.setLimitKey(macro, keybind, InputConstants.UNKNOWN);
                reload();
            }
            else {
                if (heldKey == null) {
                    heldKey = key;
                }
                else {
                    if (key != heldKey) {
                        profile.setKey(macro, keybind, key);
                        profile.setLimitKey(macro, keybind, heldKey);
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
        if (macro != null && keybind != null) {
            if (heldKey == key) {
                profile.setKey(macro, keybind, key);
                profile.setLimitKey(macro, keybind, InputConstants.UNKNOWN);
                reload();
                return true;
            }
        }
        else if (key.equals(sendKey)) {
            if (getSelected() == null && CommandKeys.inGame()) {
                Collection<Keybind> keybinds = profile.keybindMap.get(key);
                Keybind active1 = null;
                Keybind active2 = null;
                for (Keybind kb : keybinds) {
                    if (kb.isLimitKeyDown()) {
                        active1 = kb;
                        break;
                    } else if (kb.getLimitKey().equals(InputConstants.UNKNOWN)) {
                        active2 = kb;
                    }
                }
                if (active1 == null) active1 = active2;
                Collection<Macro> macros = profile.macroMap.get(active1);
                if (!macros.isEmpty()) {
                    screen.onClose();
                    minecraft.setScreen(null);
                    Keybind trigger = active1;
                    macros.forEach((macro) -> macro.trigger(trigger));
                    return true;
                }
            }
            sendKey = null;
        }
        // TODO null keys?
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
        minecraft.setScreen(new KeyBindsScreen(screen, minecraft.options));
    }
}
