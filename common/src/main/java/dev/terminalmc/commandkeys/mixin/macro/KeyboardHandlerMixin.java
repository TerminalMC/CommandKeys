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

package dev.terminalmc.commandkeys.mixin.macro;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.util.KeybindUtil;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {

    @Unique
    private static boolean commandKeys$cancelKeyPressed;

    @Unique
    private static long commandKeys$cancelKeyPressedTime;

    @Unique
    private static boolean commandKeys$cancelCharTyped;

    @Unique
    private static long commandKeys$cancelCharTypedTime;

    /**
     * Passes keyboard key press to {@link KeybindUtil#handleKey} and allows it to be canceled
     * before being passed to the Minecraft callback.
     *
     * @see MouseHandlerMixin#wrapClick
     */
    @WrapOperation(
            method = "keyPress",
            at = @At(
                    value = "INVOKE:LAST",
                    target = "Lnet/minecraft/client/KeyMapping;set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V"
            )
    )
    @SuppressWarnings("JavadocReference")
    private void wrapSet(InputConstants.Key key, boolean held, Operation<Void> original) {
        int cancel = KeybindUtil.handleKey(key);

        commandKeys$cancelKeyPressed = (cancel == 2);
        if (commandKeys$cancelKeyPressed)
            commandKeys$cancelKeyPressedTime = System.nanoTime();

        commandKeys$cancelCharTyped = (cancel != 0);
        if (commandKeys$cancelCharTyped)
            commandKeys$cancelCharTypedTime = System.nanoTime();

        if (!commandKeys$cancelKeyPressed) {
            original.call(key, held);
        }
    }

    /**
     * Allows cancellation of the call to {@link net.minecraft.client.KeyMapping#click}
     * corresponding to a call canceled by {@link KeyboardHandlerMixin#wrapSet}.
     */
    @WrapOperation(
            method = "keyPress",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V"
            )
    )
    private void wrapClick(InputConstants.Key key, Operation<Void> original) {
        if (commandKeys$cancelKeyPressed) {
            commandKeys$cancelKeyPressed = false;
            // Cancel only if the most recent cancelling set
            // was less than 5 milliseconds ago
            if (System.nanoTime() - commandKeys$cancelKeyPressedTime < 5_000_000)
                return;
        }
        original.call(key);
    }

    /**
     * Allows cancellation of the call to {@link KeyboardHandler#charTyped} corresponding to a call
     * canceled by {@link KeyboardHandlerMixin#wrapSet}.
     */
    @WrapMethod(method = "charTyped")
    @SuppressWarnings("JavadocReference")
    private void wrapCharTyped(long windowPointer, CharacterEvent event, Operation<Void> original) {
        if (commandKeys$cancelCharTyped) {
            commandKeys$cancelCharTyped = false;
            // Cancel only if the most recent cancelling set
            // was less than 5 milliseconds ago
            if (System.nanoTime() - commandKeys$cancelCharTypedTime < 5_000_000)
                return;
        }
        original.call(windowPointer, event);
    }
}
