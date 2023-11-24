package notryken.quickmessages.mixin;

import net.minecraft.client.KeyboardHandler;
import notryken.quickmessages.QuickMessages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {
    @Inject(method = "keyPress", at = @At("TAIL"))
    private void keyPress(long $$0, int $$1, int $$2, int $$3, int $$4, CallbackInfo ci) {
        QuickMessages.onInput();
    }
}
