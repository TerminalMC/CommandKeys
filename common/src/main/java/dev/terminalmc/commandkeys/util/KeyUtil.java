/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.util;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.CommandKey;
import dev.terminalmc.commandkeys.config.Message;
import dev.terminalmc.commandkeys.config.Profile;
import dev.terminalmc.commandkeys.config.QuadState;
import dev.terminalmc.commandkeys.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        boolean cancel = false;
        boolean cancelNext = false;

        // Check that we can send, and whether there's any matching CommandKey
        if (Minecraft.getInstance().screen == null && profile().commandKeyMap.containsKey(key)) {
            // Get all keys matching the pressed key

            /*
            0. b
            1. Get all CKs matching the pressed key
            2. Check modifiers; if any modified CK has its modifier down,


             */

            long window = Minecraft.getInstance().getWindow().getWindow();
            boolean onlyLimit = false;

            Collection<CommandKey> allKeys = profile().commandKeyMap.get(key);

            // If any matching CommandKey has a pressed limit key, don't send
            // messages of any CommandKeys without limit keys.

            for (CommandKey cmdKey : allKeys) {
                if (!cmdKey.conflictStrategy.state.equals(QuadState.State.THREE)) {
                    if (!cmdKey.getLimitKey().equals(InputConstants.UNKNOWN)) {
                        if (InputConstants.isKeyDown(window, cmdKey.getLimitKey().getValue())) {
                            onlyLimit = true;
                            break;
                        }
                    }
                }
            }

            List<CommandKey> sendKeys = new ArrayList<>();
            if (onlyLimit) {
                // Only send for limit-capable CommandKeys with limit key pressed
                for (CommandKey cmdKey : allKeys) {
                    if (!cmdKey.conflictStrategy.state.equals(QuadState.State.THREE)) {
                        if (!cmdKey.getLimitKey().equals(InputConstants.UNKNOWN)) {
                            if (InputConstants.isKeyDown(window, cmdKey.getLimitKey().getValue())) {
                                sendKeys.add(cmdKey);
                            }
                        }
                    }
                }
            }
            else {
                // Only send for non-limit-capable CommandKeys
                for (CommandKey cmdKey : allKeys) {
                    if (!cmdKey.conflictStrategy.state.equals(QuadState.State.THREE)) {
                        if (cmdKey.getLimitKey().equals(InputConstants.UNKNOWN)) {
                            sendKeys.add(cmdKey);
                        }
                    }
                }
            }

            boolean override = false;
            for (CommandKey cmdKey : sendKeys) {
                boolean send = true;
                switch(cmdKey.conflictStrategy.state) {
                    // Can't use MAP.contains(key) because Forge replaces the
                    // java.util.Map with a KeyMappingLookup thing.
                    case ZERO -> send = getConflict(key) == null;
                    case TWO -> override = true;
                }

                if (send) {
                    cancelNext = true;

                    switch(cmdKey.sendStrategy.state) {
                        case ZERO -> {
                            Profile profile = CommandKeys.profile();
                            // If using standard delay, doesn't apply to first
                            boolean standardDelay = cmdKey.spaceTicks != -1;
                            int cumulativeDelay = standardDelay ? -cmdKey.spaceTicks : 0;
                            for (Message msg : cmdKey.getMessages()) {
                                cumulativeDelay += standardDelay ? cmdKey.spaceTicks : msg.delayTicks;
                                CommandKeys.queue(cumulativeDelay, msg.string,
                                        profile.addToHistory, profile.showHudMessage);
                            }
                        }
                        case ONE -> {
                            if (!cmdKey.getMessages().isEmpty()) {
                                cancel = true;
                                CommandKeys.type(cmdKey.getMessages().getFirst().string);
                            }
                        }
                        case TWO -> {
                            // Allow spacer blank messages, and multiple
                            // messages per cycling key-press.
                            Profile profile = CommandKeys.profile();
                            String messages = cmdKey.getMessages().get(cmdKey.cycleIndex).string;
                            for (String msg : messages.split(",,")) {
                                if (!msg.isBlank()) {
                                    CommandKeys.send(msg, profile.addToHistory, profile.showHudMessage);
                                }
                            }
                            if (cmdKey.cycleIndex < cmdKey.getMessages().size() - 1) {
                                cmdKey.cycleIndex ++;
                            } else {
                                cmdKey.cycleIndex = 0;
                            }
                        }
                    }
                }
            }
            if (override) cancel = override;
        }
        if (!cancel) KeyMapping.click(key);
        return cancelNext;
    }
}
