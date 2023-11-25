package notryken.commandkeys.mixin;

import net.minecraft.client.Minecraft;
import notryken.commandkeys.CommandKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "onResourceLoadFinished", at = @At("TAIL"))
    private void checkDuplicates(CallbackInfo ci) {
        /*
        Required in case CommandKeys config is initialized before
        Minecraft.options
         */
        try {
            CommandKeys.config().checkDuplicatesMono();
        }
        catch (IllegalStateException e) {
            // Pass
        }
    }
}
