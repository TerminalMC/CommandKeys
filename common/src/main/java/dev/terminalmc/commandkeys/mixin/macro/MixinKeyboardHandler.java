/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.mixin.macro;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.util.KeybindUtil;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {
    @Unique
    private static boolean commandKeys$cancelCharTyped;

    @WrapOperation(
            method = "keyPress",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V"
            )
    )
    private void wrapClick(InputConstants.Key keymapping, Operation<Void> original) {
        int cancel = KeybindUtil.handleKey(keymapping);
        commandKeys$cancelCharTyped = (cancel != 0);
        if (cancel != 2) original.call(keymapping);
    }

    @WrapMethod(method = "charTyped")
    private void wrapCharTyped(long windowPointer, int codePoint, int modifiers, Operation<Void> original) {
        if (commandKeys$cancelCharTyped) commandKeys$cancelCharTyped = false;
        else original.call(windowPointer, codePoint, modifiers);
    }
}
