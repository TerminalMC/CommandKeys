package notryken.commandkeys.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import notryken.commandkeys.gui.component.listwidget.ConfigListWidget;
import notryken.commandkeys.gui.component.listwidget.ConfigListWidgetMono;

public class ConfigScreenMono extends ConfigScreen {

    public ConfigScreenMono(Screen parent, Options options, Component title, ConfigListWidget listWidget) {
        super(parent, options, title, listWidget);
    }

    @Override
    protected void init() {
        if (listWidget == null) {
            listWidget = new ConfigListWidgetMono(Minecraft.getInstance(), width, height,
                    32, height - 32, 25, lastScreen, title);
        }
        super.init();
    }
}
