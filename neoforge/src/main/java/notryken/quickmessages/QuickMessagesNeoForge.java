package notryken.quickmessages;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.ConfigScreenHandler;

@Mod(Constants.MOD_ID)
public class QuickMessagesNeoForge {
public QuickMessagesNeoForge() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> new ConfigScreenDual(parent))
                );

        QuickMessages.init();
    }
}