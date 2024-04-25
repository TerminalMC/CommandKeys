/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package com.notryken.commandkeys.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.notryken.commandkeys.util.KeyUtil;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {
    private static boolean cancelCharTyped;

    @Redirect(
            method = "keyPress",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V"
            )
    )
    private void onKeyPress(InputConstants.Key key) {
        cancelCharTyped = KeyUtil.handleKey(key);
    }

    @Inject(
            method = "charTyped",
            at = @At("HEAD"),
            cancellable = true
    )
    private void charTyped(long window, int keyCode, int scanCode, CallbackInfo ci) {
        if (cancelCharTyped) {
            cancelCharTyped = false;
            ci.cancel();
        }
    }
}
