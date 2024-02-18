package com.notryken.commandkeys.gui.component.listwidget;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.notryken.commandkeys.CommandKeys;
import com.notryken.commandkeys.gui.screen.ConfigScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@code ConfigListWidget} is tightly coupled to a {@code ConfigScreen}, and
 * is used to avoid the requirement for each different configuration screen
 * to require a unique {@code Screen} implementation.
 * <p>
 * A {@code ConfigListWidget} has a list of {@code ConfigListWidget.Entry}
 * objects, which are drawn onto the screen top-down in the order that they
 * are stored, with standard spacing.
 * <p>
 * <b>Note:</b> If you want multiple components (e.g. buttons, text fields) to
 * appear side-by-side rather than spaced vertically, you must add them all to a
 * single Entry's list of {@code AbstractWidgets}.
 */
public abstract class ConfigListWidget extends ContainerObjectSelectionList<ConfigListWidget.Entry> {

    protected ConfigScreen screen;
    // Standard positional and dimensional values used by entries
    protected final int entryRelX;
    protected final int entryX;
    protected final int entryWidth;
    protected final int entryHeight;
    protected final int scrollWidth;

    public ConfigListWidget(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight,
                            int entryRelX, int entryWidth, int entryHeight, int scrollWidth) {
        super(minecraft, width, height, top, bottom, itemHeight);
        this.entryRelX = entryRelX;
        this.entryX = width / 2 + entryRelX;
        this.entryWidth = entryWidth;
        this.entryHeight = entryHeight;
        this.scrollWidth = scrollWidth;
    }

    @Override
    public int getRowWidth() {
        // Sets the clickable width
        return scrollWidth;
    }

    @Override
    protected int getScrollbarPosition() {
        // Sets the scrollbar position
        return width / 2 + scrollWidth / 2;
    }

    /**
     * Must be called when the {@code ConfigListWidget} is added to a
     * {@code Screen}, else breaks.
     */
    public void setScreen(ConfigScreen screen) {
        this.screen = screen;
    }

    public void reload() {
        screen.reload();
    }

    // Abstract methods
    public abstract ConfigListWidget resize(int width, int height, int top, int bottom,
                                            int itemHeight, double scrollAmount);

    public abstract boolean keyPressed(InputConstants.Key key);
    public abstract boolean keyReleased(InputConstants.Key key);
    public abstract boolean mouseClicked(InputConstants.Key key);
    public abstract boolean mouseReleased(InputConstants.Key key);

    /**
     * Base implementation of options list widget entry, with common entries.
     */
    public abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        public static final ResourceLocation COLLAPSE_ICON = new ResourceLocation(CommandKeys.MOD_ID, "textures/gui/collapse_button.png");
        public static final ResourceLocation CONFIGURE_ICON = new ResourceLocation(CommandKeys.MOD_ID, "textures/gui/configure_button.png");
        public static final ResourceLocation COPY_ICON = new ResourceLocation(CommandKeys.MOD_ID, "textures/gui/copy_button.png");


        public final List<AbstractWidget> elements;

        public Entry() {
            this.elements = new ArrayList<>();
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return elements;
        }

        @Override
        public @NotNull List<? extends NarratableEntry> narratables() {
            return elements;
        }

        @Override
        public void render(@NotNull GuiGraphics context, int index, int y, int x,
                           int entryWidth, int entryHeight, int mouseX, int mouseY,
                           boolean hovered, float tickDelta) {
            elements.forEach((button) -> {
                button.setY(y);
                button.render(context, mouseX, mouseY, tickDelta);
            });
        }

        // Common Entry implementations

        public static class TextEntry extends Entry {
            public TextEntry(int x, int width, int height, Component message,
                             @Nullable Tooltip tooltip, int tooltipDelay) {
                super();

                AbstractStringWidget widget;
                if (Minecraft.getInstance().font.width(message.getString()) <= width) {
                    widget = new StringWidget(x, 0, width, height, message, Minecraft.getInstance().font);
                } else {
                    widget = new MultiLineTextWidget(x, 0, message, Minecraft.getInstance().font)
                            .setMaxWidth(width)
                            .setCentered(true);
                }
                if (tooltip != null) widget.setTooltip(tooltip);
                if (tooltipDelay >= 0) widget.setTooltipDelay(tooltipDelay);

                elements.add(widget);
            }
        }

        public static class ActionButtonEntry extends Entry {
            public ActionButtonEntry(int x, int y, int width, int height,
                                     Component message, @Nullable Tooltip tooltip,
                                     int tooltipDelay, Button.OnPress onPress) {
                super();

                Button button = Button.builder(message, onPress)
                        .pos(x, y)
                        .size(width, height)
                        .build();
                if (tooltip != null) button.setTooltip(tooltip);
                if (tooltipDelay >= 0) button.setTooltipDelay(tooltipDelay);

                elements.add(button);
            }
        }
    }
}