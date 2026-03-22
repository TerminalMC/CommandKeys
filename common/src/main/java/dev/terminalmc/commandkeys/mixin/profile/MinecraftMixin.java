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

package dev.terminalmc.commandkeys.mixin.profile;

import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    /**
     * Automatic profile switching for singleplayer.
     */
    @Inject(
            method = "doWorldLoad",
            at = @At("HEAD")
    )
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void startIntegratedServer(
            LevelStorageSource.LevelStorageAccess levelSourceAccess,
            PackRepository packRepository,
            WorldStem worldStem,
            Optional<GameRules> gameRules,
            boolean newWorld,
            CallbackInfo ci
    ) {
        String world = worldStem.worldDataAndGenSettings().data().getLevelName();
        Config.get().activateSpProfile(world);
        CommandKeys.lastConnection = world;
    }
}
