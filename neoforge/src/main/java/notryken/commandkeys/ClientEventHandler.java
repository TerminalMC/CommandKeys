package notryken.commandkeys;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.TickEvent;

public class ClientEventHandler {
    @SubscribeEvent
    public static void clientTickEvent(TickEvent.ClientTickEvent event) {
        if(TickEvent.Phase.START.equals(event.phase)) {
            CommandKeys.onEndTick(Minecraft.getInstance());
        }
    }
}
