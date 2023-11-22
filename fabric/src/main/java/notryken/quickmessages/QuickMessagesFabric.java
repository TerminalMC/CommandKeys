package notryken.quickmessages;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public class QuickMessagesFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        KeyBindingHelper.registerKeyBinding(QuickMessages.CONFIG_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(QuickMessages::onEndTick);

        QuickMessages.init();
    }
}