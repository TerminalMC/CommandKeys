package notryken.quickmessages.mixin;

import net.minecraft.client.Minecraft;
import notryken.quickmessages.QuickMessages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "onResourceLoadFinished", at = @At("TAIL"))
    private void checkDuplicates(CallbackInfo ci) {
        QuickMessages.config().checkDuplicatesMono();
    }
}
