package dev.terminalmc.commandkeys.mixin.profile;

import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    /**
     * Automatic profile switching for singleplayer.
     */
    @Inject(
            method = "doWorldLoad",
            at = @At("HEAD")
    )
    private void startIntegratedServer(LevelStorageSource.LevelStorageAccess levelStorage,
                                       PackRepository packRepo, WorldStem worldStem,
                                       boolean newWorld, CallbackInfo ci) {
        String world = worldStem.worldData().getLevelName();
        Config.get().activateSpProfile(world);
        CommandKeys.lastConnection = world;
    }
}
