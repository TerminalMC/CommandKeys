package dev.terminalmc.commandkeys.mixin.profile;

import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
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
                                          TransferState state, CallbackInfo ci) {
        String server = address.getHost();
        Config.get().activateMpProfile(server);
        CommandKeys.lastConnection = server;
    }
}
