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

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@Mod(value = CommandKeys.MOD_ID)
@EventBusSubscriber(
        modid = CommandKeys.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class CommandKeysForge {

    public CommandKeysForge() {
        // Register config screen
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> CommandKeys.getConfigScreen(parent))
        );

        // Initialize client
        CommandKeys.init();
    }

    /**
     * Registers all keybinds.
     */
    @SubscribeEvent
    static void registerKeyMappingsEvent(RegisterKeyMappingsEvent event) {
        CommandKeys.KEYBINDS.forEach(event::register);
    }

    @EventBusSubscriber(
            modid = CommandKeys.MOD_ID,
            value = Dist.CLIENT
    )
    static class ClientEventHandler {

        /**
         * Registers client after-tick event.
         */
        @SubscribeEvent
        public static void registerAfterClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase.equals(TickEvent.Phase.END)) {
                CommandKeys.afterClientTick(Minecraft.getInstance());
            }
        }
    }
}
