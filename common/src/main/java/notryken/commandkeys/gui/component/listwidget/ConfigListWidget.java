package notryken.commandkeys.gui.component.listwidget;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation of CommandKeys options list widget.
 * <p>
 * A {@code ConfigListWidget} has a list of {@code ConfigListWidget.Entry}
 * objects, which are drawn onto the screen top-down in the order that they
 * are stored, with standard spacing.
 * <p>
 * <b>Note:</b> if you want multiple components (e.g. buttons, text fields) to
 * appear side-by-side rather than spaced vertically, you must add them all to a
 * single Entry's list of {@code AbstractWidgets}.
 */
public abstract class ConfigListWidget
        extends ContainerObjectSelectionList<ConfigListWidget.Entry> {

    public final Minecraft client;
    public final Screen parentScreen;
    public final Component screenTitle;
    public final int top;
    public final int bottom;

    public ConfigListWidget(Minecraft client, int width, int height, int top, int bottom,
                            int itemHeight, Screen parentScreen, Component screenTitle) {
        super(client, width, height, top, bottom, itemHeight);
        this.client = client;
        this.parentScreen = parentScreen;
        this.screenTitle = screenTitle;
        this.top = top;
        this.bottom = bottom;
    }

    // Default methods

    @Override
    public int getRowWidth() {
        // Sets the position of the scrollbar
        return 400;
    }

    @Override
    protected int getScrollbarPosition() {
        // Offset as a buffer
        return super.getScrollbarPosition() + 82;
    }

    // Abstract methods

    protected abstract void reloadScreen();
    public abstract boolean keyPressed(InputConstants.Key key);
    public abstract boolean handleKey(InputConstants.Key key);
    public abstract ConfigListWidget resize(int width, int height, int top, int bottom);

    /**
     * Base implementation of ChatNotify options list widget entry, with common
     * entries.
     */
    public abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {

        public final List<AbstractWidget> options = new ArrayList<>();

        @Override
        public void render(@NotNull GuiGraphics context, int index, int y, int x,
                           int entryWidth, int entryHeight, int mouseX, int mouseY,
                           boolean hovered, float tickDelta) {
            options.forEach((button) -> {
                button.setY(y);
                button.render(context, mouseX, mouseY, tickDelta);
            });
        }

        @Override
        @NotNull
        public List<? extends GuiEventListener> children() {
            return this.options;
        }

        @Override
        @NotNull
        public List<? extends NarratableEntry> narratables() {
            return this.options;
        }

        // Default Entry implementations

        /**
         * A {@code Header} is a {@code StringWidget} with position and
         * dimensions set to standard CommandKeys values.
         */
        public static class Header extends Entry {
            public Header(int width, Minecraft client, Component label, Component tooltip) {
                super();
                StringWidget header = new StringWidget(width / 2 - 200, 0, 400, 20, label, client.font);
                header.setTooltip(tooltip == null ? null : Tooltip.create(tooltip));
                header.setTooltipDelay(500);
                options.add(header);
            }
        }

    }
}
