package notryken.quickmessages.mixin;

import net.minecraft.client.MinecraftClient;
import notryken.quickmessages.client.QuickMessagesClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient
{
    @Inject(at = @At("HEAD"), method = "close")
    private void close(CallbackInfo ci)
    {
        QuickMessagesClient.saveConfig();
    }
}
