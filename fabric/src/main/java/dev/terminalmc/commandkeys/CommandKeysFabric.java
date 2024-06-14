package dev.terminalmc.commandkeys;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public class CommandKeysFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Keybindings
        KeyBindingHelper.registerKeyBinding(CommandKeys.CONFIG_KEY);

        // Tick events
        ClientTickEvents.END_CLIENT_TICK.register(CommandKeys::onEndTick);

        // Main initialization
        CommandKeys.init();
    }
}
