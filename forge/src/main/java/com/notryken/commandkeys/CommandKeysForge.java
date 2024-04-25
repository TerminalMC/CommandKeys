/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package com.notryken.commandkeys;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(CommandKeys.MOD_ID)
public class CommandKeysForge {
    public CommandKeysForge() {
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
