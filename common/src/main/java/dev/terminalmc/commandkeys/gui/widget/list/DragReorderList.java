/*
 * Copyright 2026 TerminalMC
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Extends {@link OptionList} to add support for re-ordering entries within a contiguous sub-list of
 * a particular entry type.
 *
 * <p>Works with multiple sub-lists, adjacent or otherwise (not overlapping),
 * but each list must be of a different {@link Entry} child class.</p>
 *
 * <p>Supports {@link Entry.Space} enabling double-size entries.</p>
 *
 * <p>Supports 'trailer' entries, where the trailer is an instance of a
 * different class.</p>
 */
public abstract class DragReorderList extends OptionList {

    private final Map<Class<? extends Entry>, BiFunction<Integer, Integer, Boolean>> clsFunMap;
    private int dragSourceSlot = -1;
    private Class<? extends Entry> dragClass;
    private @Nullable Class<? extends Entry> trailerClass;
    boolean hasTrailer;

    public DragReorderList(
            Minecraft mc,
            int width,
            int height,
            int y,
            int entryWidth,
            int entryHeight,
            int entrySpacing,
            Map<Class<? extends Entry>, BiFunction<Integer, Integer, Boolean>> clsFunMap
    ) {
        super(mc, width, height, y, entryWidth, entryHeight, entrySpacing);
        this.clsFunMap = clsFunMap;
    }

    protected Entry getEntry(int index) {
        return this.children().get(index);
    }

    /**
     * <p>You should mark the {@code entry} as being dragged via
     * {@link Entry#setDragging} before invoking this method.</p>
     *
     * <p>{@code entry} must be an instance of a class present in the map passed
     * in the constructor.</p>
     *
     * <p>{@code trailerClass} must be set if any entries in the contiguous list
     * may have trailer entries.</p>
     *
     * @param entry        the entry being dragged.
     * @param trailerClass the class of entries which may be attached as 'trailers' to the primary
     *                     entries.
     * @param hasTrailer   whether {@code entry} has a trailer.
     */
    @SuppressWarnings("SameParameterValue")
    protected void startDragging(
            Entry entry,
            @Nullable Class<? extends Entry> trailerClass,
            boolean hasTrailer
    ) {
        if (validate(entry, trailerClass, hasTrailer)) {
            this.dragSourceSlot = children().indexOf(entry);
            this.dragClass = entry.getClass();
            this.trailerClass = trailerClass;
            this.hasTrailer = hasTrailer;
        }
    }

    /**
     * Determines whether the list structure is valid for the given dragging parameters.
     */
    private boolean validate(
            Entry entry,
            @Nullable Class<? extends Entry> trailerClass,
            boolean hasTrailer
    ) {
        Class<? extends Entry> cls = entry.getClass();

        // Verify that entry is present in list
        int index = children().indexOf(entry);
        if (index == -1) {
            CommandKeys.LOG.error("Cannot drag entry of type '{}'. Not present in list.");
            return false;
        }

        // Verify that entry's class is not the same as trailerClass
        if (cls.equals(trailerClass)) {
            CommandKeys.LOG.error(
                    "Cannot drag entry of type '{}' at index {}. " + "Identical trailer class.",
                    cls,
                    index
            );
            return false;
        }

        // Verify that we have a function for entry's class
        if (!clsFunMap.containsKey(entry.getClass())) {
            CommandKeys.LOG.error(
                    "Cannot drag entry of type '{}' at index {}. Allowed types: {}",
                    cls,
                    index,
                    clsFunMap.keySet()
                            .stream()
                            .map((Class::getName))
                            .collect(Collectors.joining(", "))
            );
            return false;
        }

        if (hasTrailer) {
            // Verify that it's possible to have a trailer
            if (index < children().size()) {
                // Verify that the trailer is a valid trailer class
                Class<? extends Entry> trailerCls = children().get(index + 1).getClass();
                if (!trailerCls.equals(Entry.Space.class) && !trailerCls.equals(trailerClass)) {
                    CommandKeys.LOG.error(
                            "Cannot drag entry of type '{}' at index {}. "
                                    + "hasTrailer is true but trailer is class '{}'. "
                                    + "Allowed trailer types: '{}', '{}'.",
                            Entry.Space.class,
                            trailerClass
                    );
                    return false;
                }
            } else {
                CommandKeys.LOG.error("Cannot drag entry of type '{}' at index {}. "
                        + "hasTrailer is true but entry is the last list element.");
                return false;
            }
        }

        // Verify that the sub-list with type matching entry is contiguous
        int start = getListStart(cls);
        int end = getListEnd(cls);
        for (int i = start; i <= end; i++) {
            if (children().get(i).getClass().equals(cls)) {
                Class<? extends Entry> trailerCls = children().get(i + 1).getClass();
                if (trailerCls.equals(trailerClass) || trailerCls.equals(Entry.Space.class)) {
                    i++; // Skip the trailer
                }
            } else {
                CommandKeys.LOG.error(
                        "Cannot drag entry of type '{}' at index {}. "
                                + "Encountered unexpected type '{}' at index {} breaking "
                                + "contiguous sub-list from {} to {}. Allowed trailer types: "
                                + "'{}', '{}'.",
                        cls,
                        children().indexOf(entry),
                        children().get(i).getClass(),
                        i,
                        start,
                        end,
                        Entry.Space.class,
                        trailerClass
                );
                return false;
            }
        }

        return true;
    }

    private void cancelDrag() {
        dragSourceSlot = -1;
    }

    @Override
    public void extractWidgetRenderState(
            @NotNull GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
        if (dragSourceSlot != -1) {
            super.extractItem(
                    graphics,
                    mouseX,
                    mouseY,
                    delta,
                    getEntry(dragSourceSlot)
            );
            if (hasTrailer) {
                super.extractItem(
                        graphics,
                        mouseX,
                        mouseY,
                        delta,
                        getEntry(dragSourceSlot + 1)
                );
            }
        }
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (dragSourceSlot != -1 && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            dropDragged(event.y());
            return true;
        }
        return super.mouseReleased(event);
    }

    /**
     * @return the index of the first instance of {@code cls} in the {@link OptionList}.
     */
    private int getListStart(Class<? extends Entry> cls) {
        for (int i = 0; i < children().size(); i++) {
            if (children().get(i).getClass().equals(cls))
                return i;
        }
        throw new IllegalArgumentException(
                "getListStart could not find any element of type " + cls.getName());
    }

    /**
     * @return the index of the last instance of {@code cls} in the {@link OptionList}.
     */
    private int getListEnd(Class<? extends Entry> cls) {
        for (int i = children().size() - 1; i >= 0; i--) {
            if (children().get(i).getClass().equals(cls))
                return i;
        }
        throw new IllegalArgumentException(
                "getListEnd could not find any element of type " + cls.getName());
    }

    /**
     * Finds the backing list index of the entry at the specified index.
     *
     * @return the number of entries of classes not matching {@link DragReorderList#dragClass}
     * before (and including) the specified index.
     */
    private int getOffset(int index) {
        int i = 0;
        int offset = 0;
        for (OptionList.Entry entry : children()) {
            if (!entry.getClass().equals(dragClass))
                offset++;
            if (i++ == index)
                return offset;
        }
        throw new IllegalArgumentException(String.format(
                "Index out of range for class '%s' and index %s",
                dragClass,
                index
        ));
    }

    /**
     * A dragged entry, when dropped, will be placed below the hovered entry. Therefore, the move
     * operation will only be executed if the hovered entry is below the dragged entry, or more than
     * one slot above.
     */
    private void dropDragged(double mouseY) {
        int start = getListStart(dragClass);
        int end = getListEnd(dragClass);
        if (start == end) {
            // Only one entry in list
            cancelDrag();
            return;
        }

        @Nullable Entry hoveredEntry = getEntryAtPosition(getX() + (double) getWidth() / 2, mouseY);
        if (hoveredEntry == null) {
            if (mouseY > getBottom() || mouseY > getY() + defaultEntryHeight * children().size()) {
                // If we're off the bottom, snap to list bottom. Now if
                // hoveredEntry is still null we can assume we're off the top.
                hoveredEntry = children().get(end);
            }
        }

        int hoveredSlot;
        if (hoveredEntry == null) {
            // Not targeting a valid element, so we snap to list top - 1
            // (previous check covers list bottom)
            hoveredSlot = start - 1;
        } else {
            // Targeting a valid element, so we get its index
            hoveredSlot = children().indexOf(hoveredEntry);

            // If outside the sub-list, snap to top or bottom
            if (hoveredSlot <= start - 1) {
                hoveredSlot = start - 1;
            } else if (hoveredSlot > end) {
                hoveredSlot = end;
            } else {
                // Within the sub-list, but it's still possible that there are
                // invalid elements here so we need to check
                if (hoveredEntry.getClass().equals(Entry.Space.class) || hoveredEntry.getClass()
                        .equals(trailerClass)) {
                    // Targeting a trailer, check that the main entry is valid
                    // and switch to it
                    if (!children().get(--hoveredSlot).getClass().equals(dragClass)) {
                        throw new IllegalStateException(String.format(
                                "Invalid list structure: trailer parent class '%s', expected '%s'",
                                children().get(hoveredSlot).getClass(),
                                dragClass
                        ));
                    }
                } else if (!hoveredEntry.getClass().equals(dragClass)) {
                    throw new IllegalStateException(String.format(
                            "Invalid list structure: found class %s, expected %s or %s",
                            hoveredEntry.getClass(),
                            dragClass,
                            trailerClass
                    ));
                }
            }
        }

        // Check whether the move operation would actually change anything
        if (hoveredSlot > dragSourceSlot || hoveredSlot < dragSourceSlot - 1) {
            // Account for the list not starting at slot 0
            int sourceIndex = dragSourceSlot - getOffset(dragSourceSlot);
            int destIndex = hoveredSlot - getOffset(hoveredSlot);
            // I can't really explain why
            if (sourceIndex > destIndex)
                destIndex += 1;
            // Move
            if (clsFunMap.get(dragClass).apply(sourceIndex, destIndex)) {
                init();
            }
        }

        cancelDrag();
    }
}
