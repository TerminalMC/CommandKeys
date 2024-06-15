package dev.terminalmc.commandkeys.mixin;

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
     * <p>Automatic profile switching for singleplayer.</p>
     */
    @Inject(method = "doWorldLoad", at = @At("HEAD"))
    private void startIntegratedServer(LevelStorageSource.LevelStorageAccess levelStorage,
                                       PackRepository packRepo, WorldStem worldStem,
                                       boolean newWorld, CallbackInfo ci) {
        String levelId = levelStorage.getLevelId();
        Config.get().activateSpProfile(levelId);
        CommandKeys.lastConnection = levelId;
    }
}
