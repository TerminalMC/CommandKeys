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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import dev.terminalmc.commandkeys.util.KeybindUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Unique
    private static boolean commandKeys$cancelClick;

    @Unique
    private static long commandKeys$cancelClickTime;

    /**
     * Passes a mouse click to {@link KeybindUtil#handleKey} and allows it to be canceled
     * before being passed to the Minecraft callback.
     *
     * @see KeyboardHandlerMixin#wrapRelease
     * @see KeyboardHandlerMixin#wrapSet
     */
    @WrapOperation(
            method = "onPress",
            at = @At(
                    value = "INVOKE:LAST",
                    target = "Lnet/minecraft/client/KeyMapping;set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V"
            )
    )
    @SuppressWarnings("JavadocReference")
    private void wrapSet(InputConstants.Key key, boolean held, Operation<Void> original) {
        int cancel = KeybindUtil.handleKey(key, true);

        commandKeys$cancelClick = (cancel == 2);
        if (commandKeys$cancelClick)
            commandKeys$cancelClickTime = System.nanoTime();

        original.call(key, held);
    }

    /**
     * Allows cancellation of the call to {@link net.minecraft.client.KeyMapping#click}
     * corresponding to a call canceled by {@link MouseHandlerMixin#wrapSet}.
     */
    @WrapOperation(
            method = "onPress",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V"
            )
    )
    private void wrapClick(InputConstants.Key key, Operation<Void> original) {
        if (commandKeys$cancelClick) {
            commandKeys$cancelClick = false;
            KeyMapping.set(key, false);
            // Cancel only if the most recent cancelling set
            // was less than 5 milliseconds ago
            if (System.nanoTime() - commandKeys$cancelClickTime < 5_000_000)
                return;
        }
        original.call(key);
    }
}
