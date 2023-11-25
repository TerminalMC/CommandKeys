package notryken.commandkeys.mixin;

import net.minecraft.client.gui.screens.controls.KeyBindsList;
import notryken.commandkeys.CommandKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBindsList.class)
public class MixinKeyBindsList {
    @Inject(method = "resetMappingAndUpdateButtons", at = @At("TAIL"))
    private void checkDuplicates(CallbackInfo ci) {
        CommandKeys.config().checkDuplicatesMono();
    }
}
