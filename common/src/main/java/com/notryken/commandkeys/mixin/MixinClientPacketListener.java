/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package com.notryken.commandkeys.mixin;

import io.netty.channel.local.LocalAddress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import com.notryken.commandkeys.CommandKeys;
import com.notryken.commandkeys.config.Profile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void selectProfile(ClientboundLoginPacket packet, CallbackInfo ci) {

        SocketAddress address = Minecraft.getInstance().player.connection.getConnection().getRemoteAddress();

        if (address instanceof InetSocketAddress netAddress) {
            Profile profile = Profile.PROFILE_MAP.get(netAddress.getHostName());
            CommandKeys.config().setActiveProfile(Objects.requireNonNullElseGet(
                    profile, () -> CommandKeys.config().getMpDefaultProfile()));
        }
        else if (address instanceof LocalAddress) {
            CommandKeys.config().setActiveProfile(CommandKeys.config().getSpDefaultProfile());
        }
        else {
            CommandKeys.config().setActiveProfile(CommandKeys.config().getMpDefaultProfile());
        }
    }
}
