/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package com.notryken.commandkeys.gui.screen;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;
import com.notryken.commandkeys.CommandKeys;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return new qmConfigScreenFactory();
    }

    private static class qmConfigScreenFactory implements ConfigScreenFactory<Screen> {
        public Screen create(Screen screen) {
            return CommandKeys.getConfigScreen(screen);
        }
    }
}