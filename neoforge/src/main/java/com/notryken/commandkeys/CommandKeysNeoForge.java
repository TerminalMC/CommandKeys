/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package com.notryken.commandkeys;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.TickEvent;

@Mod(CommandKeys.MOD_ID)
public class CommandKeysNeoForge {
    public CommandKeysNeoForge() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> CommandKeys.getConfigScreen(parent)
                ));

        CommandKeys.init();
    }

    @SubscribeEvent
    public void registerKeyMappingsEvent(RegisterKeyMappingsEvent event) {
        event.register(CommandKeys.CONFIG_KEY);
    }

    @Mod.EventBusSubscriber(modid = CommandKeys.MOD_ID, value = Dist.CLIENT)
    static class ClientEventHandler {
        @SubscribeEvent
        public static void clientTickEvent(TickEvent.ClientTickEvent event) {
            if(event.phase.equals(TickEvent.Phase.END)) {
                CommandKeys.onEndTick(Minecraft.getInstance());
            }
        }
    }
}
