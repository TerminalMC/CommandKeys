package notryken.commandkeys;

import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientEventHandler {
    @SubscribeEvent
    public static void clientTickEvent(ClientTickEvent event) {
        if(Phase.START.equals(event.phase)) {
            CommandKeys.onEndTick(Minecraft.getInstance());
        }
    }
}
