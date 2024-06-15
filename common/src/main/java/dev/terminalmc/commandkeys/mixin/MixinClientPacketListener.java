/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.mixin;

import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.Config;
import io.netty.channel.local.LocalAddress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void selectProfile(ClientboundLoginPacket packet, CallbackInfo ci) {

        SocketAddress address = Minecraft.getInstance().player.connection.getConnection().getRemoteAddress();

        Config config = Config.get();
        if (address instanceof InetSocketAddress netAddress) {
            String name = netAddress.getHostName();
            config.activateMpProfile(name);
            CommandKeys.lastConnection = name;
        }
        else if (!(address instanceof LocalAddress)) {
            config.activateProfile(config.mpDefault);
        }
    }
}
