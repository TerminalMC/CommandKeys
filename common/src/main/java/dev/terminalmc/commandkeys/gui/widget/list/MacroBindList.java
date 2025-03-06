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
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Extends {@link DragReorderList} to add support for handling of key presses
 * and mouse button clicks for setting keybinds and triggering macros.
 */
public abstract class MacroBindList extends DragReorderList {
    protected @NotNull Profile profile;
    private @Nullable Macro macro;
    private @Nullable Keybind keybind;
    private @Nullable InputConstants.Key heldKey;
    private @Nullable Keybind sendKeybind;

    public MacroBindList(Minecraft mc, int width, int height, int top, int bottom, int entryWidth,
                         int entryHeight, int entrySpace, @NotNull Profile profile,
                         Map<Class<? extends Entry>, BiFunction<Integer,Integer,Boolean>> clsFunMap) {
        super(mc, width, height, top, bottom, entryWidth, entryHeight, entrySpace, clsFunMap);
        this.profile = profile;
    }

    @Override
    public void init() {
        super.init();
        macro = null;
        keybind = null;
        heldKey = null;
        sendKeybind = null;
    }

    protected void setSelected(@NotNull Macro macro, @NotNull Keybind keybind) {
        if (!profile.getMacros().contains(macro)) throw new IllegalArgumentException(
                "Specified macro does not exist in profile.");
        if (!macro.ownsKeybind(keybind)) throw new IllegalArgumentException(
                "Specified keybind not used by specified macro.");
        this.macro = macro;
        this.keybind = keybind;
    }

    @Override
    public boolean keyPressed(InputConstants.Key key) {
        // If we have a keybind widget selected
        if (macro != null && keybind != null) {
            if (key.getValue() == InputConstants.KEY_ESCAPE) {
                // Unbind key
                profile.setKey(macro, keybind, InputConstants.UNKNOWN);
                profile.setLimitKey(macro, keybind, InputConstants.UNKNOWN);
                init();
            }
            else {
                // If we aren't already holding a key
                if (heldKey == null) {
                    // Mark the current key as held
                    heldKey = key;
                }
                else if (key != heldKey) {
                    // Bind the current key and held key
                    profile.setKey(macro, keybind, key);
                    profile.setLimitKey(macro, keybind, heldKey);
                    init();
                }
                return false;
            }
            return true;
        }
        // Else if we have no other widget selected
        else if (getSelected() == null && CommandKeys.inGame() &&
                !key.equals(((KeyMappingAccessor) CommandKeys.CONFIG_KEY).getKey())) {
            // Prepare to use the key to trigger macros on release
            Collection<Keybind> keybinds = profile.keybindMap.get(key);
            Keybind limitedKb = null;
            Keybind monoKb = null;
            for (Keybind kb : keybinds) {
                if (kb.isLimitKeyDown()) {
                    limitedKb = kb;
                    break;
                } else if (kb.getLimitKey().equals(InputConstants.UNKNOWN)) {
                    monoKb = kb;
                }
            }
            sendKeybind = limitedKb != null ? limitedKb : monoKb;
        }
        return false;
    }

    @Override
    public boolean keyReleased(InputConstants.Key key) {
        // If we have a keybind widget selected
        if (macro != null && keybind != null) {
            // If key released with no other key held
            if (heldKey == key) {
                // Bind single key
                profile.setKey(macro, keybind, key);
                profile.setLimitKey(macro, keybind, InputConstants.UNKNOWN);
                init();
                return true;
            }
        }
        else if (sendKeybind != null && sendKeybind.getKey().equals(key)) {
            // Trigger macro
            Collection<Macro> macros = profile.macroMap.get(sendKeybind);
            if (!macros.isEmpty()) {
                screen.onClose();
                minecraft.setScreen(null);
                macros.forEach((macro) -> macro.trigger(sendKeybind, false));
                return true;
            }
            sendKeybind = null;
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
        minecraft.setScreen(new KeyBindsScreen(screen, minecraft.options));
    }
}
