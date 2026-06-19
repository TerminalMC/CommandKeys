/*
 * Copyright 2026 TerminalMC
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

package dev.terminalmc.commandkeys;

import dev.terminalmc.commandkeys.command.Commands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

@SuppressWarnings("unused")
public class CommandKeysFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Initialize client
        CommandKeys.init();

        // Register keybinds
        CommandKeys.getKeybinds().forEach(KeyMappingHelper::registerKeyMapping);

        // Register client commands
        ClientCommandRegistrationCallback.EVENT.register(Commands::register);

        // Register client after-tick event
        ClientTickEvents.END_CLIENT_TICK.register(CommandKeys::afterClientTick);
    }
}
