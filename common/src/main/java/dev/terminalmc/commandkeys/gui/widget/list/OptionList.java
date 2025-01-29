/*
 * Copyright 2025 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.terminalmc.commandkeys.gui.widget.list;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.gui.screen.OptionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Tightly coupled to a generic {@link OptionsScreen}, allowing many unique
 * options screens to use a single screen implementation, while displaying
 * different options.
 *
 * <p>Contains list of {@link Entry} objects, which are drawn onto the screen
 * top-down in the order that they are stored, with each entry being allocated
 * a standard amount of space specified by {@link OptionList#itemHeight}. The
 * actual height of list entries, specified by {@link OptionList#entryHeight},
 * can be less but should not be more.</p>
 *
 * <p><b>Note:</b> If you want multiple widgets to appear side-by-side, you must
 * add them all to a single {@link Entry}'s list of widgets, which are all
 * rendered at the same list level.</p>
 */
public abstract class OptionList extends ContainerObjectSelectionList<OptionList.Entry> {
    public static final int ROW_WIDTH_MARGIN = 20;

    protected OptionsScreen screen;

    // Standard positional and dimensional values used by entries
    protected final int rowWidth;
    protected final int entryWidth;
    protected final int dynEntryWidth;
    protected final int entryHeight;
    protected final int entryX;
    protected final int dynEntryX;

    protected final int smallButtonWidth;

    public OptionList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight,
                      int entryWidth, int entryHeight) {
        super(mc, width, height, top, bottom, itemHeight);
        this.entryWidth = entryWidth;
        this.dynEntryWidth = Math.max(entryWidth, (int)(width / 5.0F * 4));
        this.entryHeight = entryHeight;
        this.entryX = width / 2 - (entryWidth / 2);
        this.dynEntryX = width / 2 - (dynEntryWidth / 2);
        this.rowWidth = Math.max(entryWidth, dynEntryWidth) + ROW_WIDTH_MARGIN;
        this.smallButtonWidth = Math.max(16, entryHeight);
    }

    @Override
    public int getRowWidth() {
        // Clickable width
        return rowWidth;
    }

    @Override
    protected int getScrollbarPosition() {
        return width / 2 + rowWidth / 2;
    }

    public OptionList reload() {
        return screen.reload();
    }

    public OptionList reload(OptionsScreen screen, int width, int height, int top, int bottom, double scrollAmount) {
        OptionList newList = reload(width, height, top, bottom, scrollAmount);
        newList.screen = screen;
        return newList;
    }

    protected abstract OptionList reload(int width, int height, int top, int bottom, double scrollAmount);

    public abstract boolean keyPressed(InputConstants.Key key);
    public abstract boolean keyReleased(InputConstants.Key key);
    public abstract boolean mouseClicked(InputConstants.Key key);
    public abstract boolean mouseReleased(InputConstants.Key key);

    /**
     * Base implementation of {@link Entry}, with common entries.
     */
    public abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        public static final int SPACING = 4;
        public static final int SMALL_SPACING = 2;

        public static final ResourceLocation COPY_ICON = new ResourceLocation(CommandKeys.MOD_ID, "textures/gui/sprites/widget/copy_button.png");
        public static final ResourceLocation OPTIONS_ICON = new ResourceLocation(CommandKeys.MOD_ID, "textures/gui/sprites/widget/options_button.png");
        public static final ResourceLocation LINK_ICON = new ResourceLocation(CommandKeys.MOD_ID, "textures/gui/sprites/widget/link_button.png");
        public static final ResourceLocation SEND_ICON = new ResourceLocation(CommandKeys.MOD_ID, "textures/gui/sprites/widget/send_button.png");

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
        public void render(@NotNull GuiGraphics graphics, int index, int y, int x,
                           int entryWidth, int entryHeight, int mouseX, int mouseY,
                           boolean hovered, float tickDelta) {
            elements.forEach((button) -> {
                button.setY(y);
                button.render(graphics, mouseX, mouseY, tickDelta);
            });
        }

        // Generic entry implementations

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
            public ActionButtonEntry(int x, int width, int height,
                                     Component message, @Nullable Tooltip tooltip,
                                     int tooltipDelay, Button.OnPress onPress) {
                super();

                Button button = Button.builder(message, onPress)
                        .pos(x, 0)
                        .size(width, height)
                        .build();
                if (tooltip != null) button.setTooltip(tooltip);
                if (tooltipDelay >= 0) button.setTooltipDelay(tooltipDelay);

                elements.add(button);
            }
        }

        /**
         * The {@link AbstractSelectionList} class (second-degree superclass of
         * {@link OptionList}) is hard-coded to only support fixed spacing of
         * entries. This is an invisible entry which defers all actions to the
         * given {@link Entry}, thereby allowing that entry to span multiple
         * slots of the {@link OptionList}.
         */
        public static class SpaceEntry extends Entry {
            private final Entry entry;

            public SpaceEntry(Entry entry) {
                super();
                this.entry = entry;
            }

            @Override
            public boolean isDragging() {
                return entry.isDragging();
            }

            @Override
            public void setDragging(boolean dragging) {
                entry.setDragging(dragging);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return entry.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button,
                                        double deltaX, double deltaY) {
                return entry.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            }

            public void setFocused(GuiEventListener listener) {
                entry.setFocused(listener);
            }

            public GuiEventListener getFocused() {
                return entry.getFocused();
            }

            public ComponentPath focusPathAtIndex(@NotNull FocusNavigationEvent event, int i) {
                if (entry.children().isEmpty()) {
                    return null;
                } else {
                    ComponentPath $$2 = entry.children().get(
                            Math.min(i, entry.children().size() - 1)).nextFocusPath(event);
                    return ComponentPath.path(entry, $$2);
                }
            }
        }
    }
}