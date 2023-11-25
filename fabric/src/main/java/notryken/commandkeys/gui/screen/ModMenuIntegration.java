package notryken.commandkeys.gui.screen;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return new qmConfigScreenFactory();
    }

    private static class qmConfigScreenFactory implements ConfigScreenFactory<Screen> {
        public Screen create(Screen screen) {
            return new ConfigScreenMono(screen, Minecraft.getInstance().options,
                    Component.translatable("screen.commandkeys.title"), null);
        }
    }
}