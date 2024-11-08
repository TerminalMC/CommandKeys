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

    /**
     * Passes keypress to {@link KeybindUtil#handleKey} and allows it to be
     * cancelled before being passed to the Minecraft callback.
     * See also {@link MixinMouseHandler#wrapClick}
     */
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

    /**
     * Allows cancellation of the call to {@link KeyboardHandler#charTyped}
     * corresponding to call cancelled in {@link KeyboardHandler#keyPress}.
     */
    @WrapMethod(method = "charTyped")
    private void wrapCharTyped(long windowPointer, int codePoint, int modifiers, 
                               Operation<Void> original) {
        if (commandKeys$cancelCharTyped) commandKeys$cancelCharTyped = false;
        else original.call(windowPointer, codePoint, modifiers);
    }
}
