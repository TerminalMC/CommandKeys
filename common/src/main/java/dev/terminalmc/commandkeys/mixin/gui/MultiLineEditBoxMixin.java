/*
 * Copyright 2025 TerminalMC
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

package dev.terminalmc.commandkeys.mixin.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.terminalmc.commandkeys.gui.widget.field.MultiLineTextField;
import net.minecraft.client.gui.components.MultiLineEditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Allows {@link MultiLineTextField} instances to edit their text color, which is normally hardcoded
 * as ARGB 0xFFE0E0E0.
 */
@Mixin(MultiLineEditBox.class)
public class MultiLineEditBoxMixin {

    @ModifyExpressionValue(
            method = "renderContents",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/components/MultiLineEditBox;textColor:I",
                    // GETFIELD
                    opcode = 180
            )
    )
    private int modifyColor(int original) {
        if ((Object) this instanceof MultiLineTextField multiLineTextField) {
            return multiLineTextField.getTextColor();
        }
        return original;
    }
}
