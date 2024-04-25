/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package com.notryken.commandkeys;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public class CommandKeysFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        KeyBindingHelper.registerKeyBinding(CommandKeys.CONFIG_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(CommandKeys::onEndTick);

        CommandKeys.init();
    }
}