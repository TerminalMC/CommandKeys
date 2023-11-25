package notryken.commandkeys;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;

public class CommandKeysQuilt implements ClientModInitializer {
    @Override
    public void onInitializeClient(ModContainer mod) {
        Constants.LOG.info("Quilt Loader Initializing CommandKeys");
        CommandKeys.init();
    }
}