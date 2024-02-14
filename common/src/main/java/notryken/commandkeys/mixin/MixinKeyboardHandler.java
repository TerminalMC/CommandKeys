package notryken.commandkeys.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import notryken.commandkeys.CommandKeys;
import notryken.commandkeys.config.CommandMonoKey;
import notryken.commandkeys.util.SendingUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static notryken.commandkeys.CommandKeys.config;

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
        cancelCharTyped = SendingUtil.handleKey(key);
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
