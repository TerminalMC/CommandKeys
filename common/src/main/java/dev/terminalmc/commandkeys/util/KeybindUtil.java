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

package dev.terminalmc.commandkeys.util;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.config.Keybind;
import dev.terminalmc.commandkeys.config.Macro;
import dev.terminalmc.commandkeys.config.Profile;
import dev.terminalmc.commandkeys.mixin.accessor.KeyMappingAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static dev.terminalmc.commandkeys.CommandKeys.profile;
import static dev.terminalmc.commandkeys.config.Macro.ConflictStrategy.AVOID;
import static dev.terminalmc.commandkeys.config.Macro.SendMode.TYPE;
import static dev.terminalmc.commandkeys.util.Localization.localized;

public class KeybindUtil {

    /**
     * Allows other mods to activate macros.
     *
     * <p>{@link InputConstants#getKey(String)} can be used to get a key from
     * a string of the format key.keyboard.h</p>
     *
     * @param key      the primary key.
     * @param limitKey the limit key.
     * @return the number of macros activated.
     */
    @SuppressWarnings("unused")
    public static int handleKeys(InputConstants.Key key, InputConstants.Key limitKey) {
        if (key.equals(InputConstants.UNKNOWN))
            return 0;
        if (!profile().keybindMap.containsKey(key))
            return 0;

        int i = 0;
        Collection<Keybind> keybinds = profile().keybindMap.get(key);
        for (Keybind keybind : keybinds) {
            if (!keybind.getLimitKey().equals(limitKey))
                continue;
            for (Macro macro : profile().macroMap.get(keybind)) {
                macro.trigger(keybind, false);
                i++;
            }
        }

        return i;
    }

    /**
     * @return the number of operations to cancel. 0 -> None. 1 -> KeyboardHandler#charTyped. 2 ->
     * KeyboardHandler#charTyped and KeyMapping#click.
     */
    public static int handleKey(InputConstants.Key key) {
        int cancel = 0;

        if (Minecraft.getInstance().screen == null && profile().keybindMap.containsKey(key)) {
            // Get all keybinds matching the pressed key
            Collection<Keybind> keybinds = profile().keybindMap.get(key);
            // The single keybind that we decide best matches the key
            Keybind triggerKb = null;

            // Search for a limited keybind for which the limit key is down
            Collection<Macro> macros = null;
            for (Keybind kb : keybinds) {
                if (kb.isLimitKeyDown()) {
                    // Found a limited keybind with its limit key down, get all
                    // macros that use this keybind and break if there are any
                    triggerKb = kb;
                    macros = profile().macroMap.get(triggerKb)
                            .stream()
                            .filter((macro) -> !macro.getStrategy().equals(AVOID))
                            .toList();
                    if (!macros.isEmpty())
                        break;
                } else if (kb.getLimitKey().equals(InputConstants.UNKNOWN)) {
                    // Save a backup in case no limited keybinds are found,
                    // avoid iterating twice
                    triggerKb = kb;
                }
            }
            // If we didn't find any limited keybinds, we use the backup
            if (macros == null || macros.isEmpty()) {
                // If we don't have a backup, we exit
                if (triggerKb == null)
                    return cancel;
                // Otherwise we get all macros matching the backup
                macros = profile().macroMap.get(triggerKb)
                        .stream()
                        .filter((macro) -> !macro.getStrategy().equals(AVOID))
                        .toList();
                // If we don't have any matching macros, we exit
                if (macros.isEmpty())
                    return cancel;
            }

            boolean ratelimited = false;

            // Trigger all matching macros
            for (Macro macro : macros) {
                boolean send = true;

                switch (macro.getStrategy()) {
                    // SUBMIT only allows sending if there's no conflict
                    case SUBMIT -> send = getConflict(key) == null;
                    // VETO requires cancelling everything
                    case VETO -> cancel = 2;
                }

                if (send) {
                    boolean rl = macro.trigger(triggerKb, ratelimited);
                    // TYPE mode requires cancelling char
                    if (!rl && cancel == 0 && macro.getMode().equals(TYPE))
                        cancel = 1;
                    ratelimited |= rl;
                }
            }
        }
        return cancel;
    }

    public static @Nullable KeyMapping getConflict(InputConstants.Key key) {
        for (KeyMapping keyMapping : Minecraft.getInstance().options.keyMappings) {
            if (((KeyMappingAccessor) keyMapping).getKey().equals(key)) {
                return keyMapping;
            }
        }
        return null;
    }

    public static class KeybindInfo {

        private final Profile profile;
        private final Macro macro;
        public MutableComponent label;
        public MutableComponent conflictLabel;
        public MutableComponent tooltip = Component.empty();
        private boolean internalConflict = false;
        private boolean mcConflict = false;

        public KeybindInfo(Profile profile, Macro macro, Keybind keybind) {
            this.profile = profile;
            this.macro = macro;
            this.label = keybind.getLimitKey().equals(InputConstants.UNKNOWN)
                    ? keybind.getKey()
                    .getDisplayName()
                    .copy()
                    : keybind.getLimitKey()
                            .getDisplayName()
                            .copy()
                            .append(" + ")
                            .append(keybind.getKey().getDisplayName());
            checkConflict(keybind.getLimitKey(), null);
            checkConflict(keybind.getKey(), keybind);
            createConflictLabel();
        }

        /**
         * Checks {@code key} against the keys used by other {@link Macro} instances, and optionally
         * against Minecraft keybinds, updating {@link KeybindInfo#internalConflict},
         * {@link KeybindInfo#mcConflict} and {@code KeybindInfo#tooltip} accordingly.
         */
        private void checkConflict(InputConstants.Key key, Keybind keybind) {
            if (key.equals(InputConstants.UNKNOWN))
                return;
            // Check internal conflict
            if (profile.keybindMap.get(key).size() > 1) {
                if (internalConflict || mcConflict)
                    tooltip.append("\n");
                tooltip.append(localized(
                        "option",
                        "macro.bind.tooltip.conflict.internal",
                        key.getDisplayName().copy().withStyle(ChatFormatting.GOLD)
                )).withStyle(ChatFormatting.WHITE);
                internalConflict = true;
            } else if (keybind != null && profile.macroMap.get(keybind).size() > 1) {
                if (internalConflict || mcConflict)
                    tooltip.append("\n");
                tooltip.append(localized(
                        "option",
                        "macro.bind.tooltip.conflict.internal",
                        key.getDisplayName().copy().withStyle(ChatFormatting.GOLD)
                )).withStyle(ChatFormatting.WHITE);
                internalConflict = true;
            }
            if (!macro.getStrategy().equals(AVOID)) {
                // Check MC conflict
                KeyMapping keyMapping = getConflict(key);
                if (keyMapping != null) {
                    if (internalConflict || mcConflict)
                        tooltip.append("\n");
                    tooltip.append(localized(
                            "option",
                            "macro.bind.tooltip.conflict.external",
                            key.getDisplayName().copy().withStyle(ChatFormatting.RED),
                            Component.translatable(keyMapping.getName())
                                    .withStyle(ChatFormatting.GRAY)
                    )).withStyle(ChatFormatting.WHITE);
                    mcConflict = true;
                }
            }
        }

        public void createConflictLabel() {
            if (mcConflict) {
                // Apply red brackets and add conflict strategy to the tooltip
                conflictLabel = Component.literal("[ ")
                        .append(label.withStyle(ChatFormatting.WHITE))
                        .append(" ]")
                        .withStyle(ChatFormatting.RED);
                tooltip.append("\n");
                tooltip.append(localized(
                        "option",
                        "macro.bind.tooltip.conflictStrategy",
                        macro.getStrategy().tooltip()
                ));
            } else if (internalConflict) {
                // Apply orange brackets
                conflictLabel = Component.literal("[ ")
                        .append(label.withStyle(ChatFormatting.WHITE))
                        .append(" ]")
                        .withStyle(ChatFormatting.GOLD);
            } else {
                // No conflict, so we use the plain label
                conflictLabel = label;
            }
        }
    }
}
