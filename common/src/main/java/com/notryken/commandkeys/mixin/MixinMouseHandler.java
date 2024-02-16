package com.notryken.commandkeys.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.notryken.commandkeys.util.KeyUtil;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Redirect(
            method = "onPress",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V"
            )
    )
    private void onButtonPress(InputConstants.Key key) {
        KeyUtil.handleKey(key);
    }
}
