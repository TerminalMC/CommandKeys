/*
 * Copyright 2024 TerminalMC
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

package dev.terminalmc.commandkeys.mixin.profile;

import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public class MixinConnectScreen {
    /**
     * Automatic profile switching for multiplayer.
     */
    @Inject(
            method = "connect",
            at = @At("HEAD")
    )
    private void selectMultiplayerProfile(Minecraft mc, ServerAddress address, ServerData data,
                                          CallbackInfo ci) {
        String server = address.getHost();
        Config.get().activateMpProfile(server);
        CommandKeys.lastConnection = server;
    }
}
