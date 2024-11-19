/*
 * CommandKeys by TerminalMC
 *
 * To the extent possible under law, the person who associated CC0 with
 * CommandKeys has waived all copyright and related or neighboring rights
 * to CommandKeys.
 *
 * You should have received a copy of the CC0 legalcode along with this
 * work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package dev.terminalmc.commandkeys;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(CommandKeys.MOD_ID)
@Mod.EventBusSubscriber(modid = CommandKeys.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CommandKeysForge {
    public CommandKeysForge() {
        // Config screen
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> CommandKeys.getConfigScreen(parent))
        );

        // Main initialization
        CommandKeys.init();
    }

    // Keybindings
    @SubscribeEvent
    static void registerKeyMappingsEvent(RegisterKeyMappingsEvent event) {
        event.register(CommandKeys.CONFIG_KEY);
    }

    @Mod.EventBusSubscriber(modid = CommandKeys.MOD_ID, value = Dist.CLIENT)
    static class ClientEventHandler {
        // Tick events
        @SubscribeEvent
        public static void clientTickEvent(TickEvent.ClientTickEvent event) {
            if (event.phase.equals(TickEvent.Phase.END)) {
                CommandKeys.onEndTick(Minecraft.getInstance());
            }
        }
    }
}
