/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.util;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.CommandKey;
import dev.terminalmc.commandkeys.config.Profile;
import dev.terminalmc.commandkeys.config.QuadState;
import dev.terminalmc.commandkeys.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static dev.terminalmc.commandkeys.CommandKeys.profile;

/**
 * <p>Key-press handling utils.</p>
 */
public class KeyUtil {
    public static @Nullable KeyMapping getConflict(InputConstants.Key key) {
        for (KeyMapping keyMapping : Minecraft.getInstance().options.keyMappings) {
            if (((KeyMappingAccessor)keyMapping).getKey().equals(key)) {
                return keyMapping;
            }
        }
        return null;
    }

    public static boolean handleKey(InputConstants.Key key) {

        boolean cancelClick = false;
        boolean cancelNext = false;
        if (Minecraft.getInstance().screen == null && profile().commandKeyMap.containsKey(key)) {

            CommandKey cmdKey = null;
            Set<CommandKey> commandKeys = profile().commandKeyMap.get(key);
            for (CommandKey ck1 : commandKeys) {
                if (!ck1.conflictStrategy.state.equals(QuadState.State.THREE)) {
                    if (ck1.getLimitKey().equals(InputConstants.UNKNOWN)) {
                        // Found a matching single-key CommandKey, but preference
                        // the ones with modifier keys that are down.
                        cmdKey = ck1;
                        for (CommandKey ck2 : commandKeys) {
                            if (!ck2.conflictStrategy.state.equals(QuadState.State.THREE) &&
                                    !ck2.getLimitKey().equals(InputConstants.UNKNOWN) &&
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
            }
            
            if (cmdKey != null) {
                
                boolean send = true;
                boolean override = false;
                switch(cmdKey.conflictStrategy.state) {
                    // Can't use MAP.contains(key) because Forge replaces the
                    // java.util.Map with a KeyMappingLookup thing.
                    case ZERO -> send = getConflict(key) == null;
                    case TWO -> override = true;
                }

                if (send) {
                    cancelNext = true;
                    cancelClick = override;

                    switch(cmdKey.sendStrategy.state) {
                        case ZERO -> {
                            Profile profile = CommandKeys.profile();
                            int i = 0;
                            for (String msg : cmdKey.messages) {
                                CommandKeys.queue(i++ * cmdKey.spaceTicks, msg,
                                        profile.addToHistory, profile.showHudMessage);
                            }
                        }
                        case ONE -> {
                            if (!cmdKey.messages.isEmpty()) {
                                cancelClick = true;
                                CommandKeys.type(cmdKey.messages.get(0));
                            }
                        }
                        case TWO -> {
                            // Strategy to allow spacer blank messages, and multiple
                            // messages per cycling key-press.
                            Profile profile = CommandKeys.profile();
                            String messages = cmdKey.messages.get(cmdKey.cycleIndex);
                            if (messages != null && !messages.isBlank()) {
                                for (String msg : messages.split(",,")) {
                                    if (!msg.isBlank()) {
                                        CommandKeys.send(msg, profile.addToHistory, profile.showHudMessage);
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
}
