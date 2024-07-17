package dev.terminalmc.commandkeys.gui.widget.list;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.config.*;
import dev.terminalmc.commandkeys.util.KeybindUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static dev.terminalmc.commandkeys.util.Localization.localized;

public class CommandKeyOptionsList extends KeybindOptionsList {
    private final CommandKey cmdKey;
    private int dragSourceSlot = -1;

    public CommandKeyOptionsList(Minecraft mc, int width, int height, int y,
                                 int itemHeight, int entryWidth, int entryHeight,
                                 CommandKey cmdKey) {
        super(mc, width, height, y, itemHeight, entryWidth, entryHeight);
        this.profile = cmdKey.profile;
        this.cmdKey = cmdKey;

        addEntry(new Entry.BindAndControlsEntry(entryX, entryWidth, entryHeight, this, cmdKey));

        addEntry(new Entry.StrategyAndModeEntry(entryX, entryWidth, entryHeight, this, cmdKey));

        addEntry(new OptionsList.Entry.TextEntry(entryX, entryWidth, entryHeight,
                localized("option", "key.messages"), null, -1));

        int i = 0;
        for (Message msg : cmdKey.getMessages()) {
            Entry e = new Entry.MessageEntry(dynEntryX, dynEntryWidth, entryHeight, this, cmdKey, i++);
            addEntry(e);
            addEntry(new OptionsList.Entry.SpaceEntry(e));
        }
        addEntry(new OptionsList.Entry.ActionButtonEntry(entryX, entryWidth, entryHeight,
                Component.literal("+"), null, -1,
                (button) -> {
                    cmdKey.addMessage(new Message());
                    reload();
                }));
    }

    @Override
    protected OptionsList reload(int width, int height, double scrollAmount) {
        CommandKeyOptionsList newListWidget = new CommandKeyOptionsList(minecraft, width, height,
                getY(), itemHeight, entryWidth, entryHeight, cmdKey);
        newListWidget.setScrollAmount(scrollAmount);
        return newListWidget;
    }

    // Message dragging

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderWidget(graphics, mouseX, mouseY, delta);
        if (dragSourceSlot != -1) {
            super.renderItem(graphics, mouseX, mouseY, delta, dragSourceSlot,
                    mouseX, mouseY, entryWidth, entryHeight);
        }
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (dragSourceSlot != -1 && button == InputConstants.MOUSE_BUTTON_LEFT) {
            dropDragged(x, y);
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    private void dropDragged(double mouseX, double mouseY) {
        OptionsList.Entry hoveredEntry = getEntryAtPosition(mouseX, mouseY);
        int hoveredSlot = children().indexOf(hoveredEntry);
        // Check whether the drop location is valid
        if (hoveredEntry instanceof Entry.MessageEntry || hoveredSlot == messageListOffset() - 1) {
            // pass
        } else if (hoveredEntry instanceof OptionsList.Entry.SpaceEntry) {
            hoveredSlot -= 1; // Reference the 'parent' Entry
        } else {
            return;
        }
        // Check whether the move operation would actually change anything
        if (hoveredSlot > dragSourceSlot || hoveredSlot < dragSourceSlot - 1) {
            // Account for the list not starting at slot 0
            int sourceIndex = dragSourceSlot - messageEntryOffset(dragSourceSlot);
            int destIndex = hoveredSlot - messageEntryOffset(hoveredSlot);
            // I can't really explain why
            if (sourceIndex > destIndex) destIndex += 1;
            // Move
            cmdKey.moveMessage(sourceIndex, destIndex);
            reload();
        }
    }

    /**
     * @return The index of the first {@link Entry.MessageEntry} in the
     * {@link OptionsList}.
     */
    private int messageListOffset() {
        int i = 0;
        for (OptionsList.Entry entry : children()) {
            if (entry instanceof Entry.MessageEntry) return i;
            i++;
        }
        throw new IllegalStateException("Response list not found");
    }

    /**
     * @return The number of non-{@link Entry.MessageEntry} entries in the
     * {@link OptionsList} before (and including) the specified index.
     */
    private int messageEntryOffset(int index) {
        int i = 0;
        int offset = 0;
        for (OptionsList.Entry entry : children()) {
            if (!(entry instanceof Entry.MessageEntry)) offset++;
            if (i++ == index) return offset;
        }
        throw new IllegalStateException("Response index out of range");
    }

    private abstract static class Entry extends OptionsList.Entry {
        private static class BindAndControlsEntry extends Entry {
            BindAndControlsEntry(int x, int width, int height, CommandKeyOptionsList listWidget,
                                 CommandKey cmdKey) {
                super();
                int buttonWidth = (width - SPACING) / 2;

                MutableComponent[] keybindInfo = KeybindUtil.getKeybindInfo(cmdKey);
                elements.add(Button.builder(keybindInfo[1],
                                (button) -> {
                                    listWidget.selectedCmdKey = cmdKey;
                                    button.setMessage(Component.literal("> ")
                                            .append(keybindInfo[0].withStyle(ChatFormatting.WHITE)
                                                    .withStyle(ChatFormatting.UNDERLINE))
                                            .append(" <").withStyle(ChatFormatting.YELLOW));
                                })
                        .tooltip(Tooltip.create(keybindInfo[2]))
                        .pos(x, 0)
                        .size(buttonWidth, height)
                        .build());

                elements.add(Button.builder(localized("option", "profile.controls"),
                                (button) -> listWidget.openMinecraftControlsScreen())
                        .pos(x + width - buttonWidth, 0)
                        .size(buttonWidth, height)
                        .build());
            }
        }

        private static class StrategyAndModeEntry extends Entry {
            StrategyAndModeEntry(int x, int width, int height, CommandKeyOptionsList listWidget,
                                 CommandKey cmdKey) {
                super();
                int buttonWidth = (width - SPACING) / 2;
                int delayFieldWidth = Minecraft.getInstance().font.width("0000000");
                int modeButtonWidth = switch(cmdKey.sendStrategy.state) {
                    case ZERO -> buttonWidth - delayFieldWidth;
                    case ONE -> buttonWidth;
                    case TWO -> buttonWidth - listWidget.smallButtonWidth;
                };
                boolean lockToSend = cmdKey.conflictStrategy.state.equals(QuadState.State.THREE);

                // Conflict strategy button
                CycleButton<QuadState.State> conflictButton = CycleButton.builder(
                        KeybindUtil::localizeConflict)
                        .withValues(QuadState.State.values())
                        .withInitialValue(cmdKey.conflictStrategy.state)
                        .withTooltip((status) -> Tooltip.create(KeybindUtil.localizeConflictTooltip(status)))
                        .create(x, 0, buttonWidth, height, localized("option", "key.conflict"),
                                (button, status) -> {
                                    cmdKey.conflictStrategy.state = status;
                                    listWidget.reload();
                                });
                elements.add(conflictButton);

                // Send mode button
                CycleButton<TriState.State> modeButton = CycleButton.<TriState.State>builder(
                        (status) -> lockToSend
                            ? localized("option", "key.mode.send")
                                    .withStyle(ChatFormatting.GREEN)
                            : KeybindUtil.localizeSendMode(status))
                        .withValues(TriState.State.values())
                        .withInitialValue(cmdKey.sendStrategy.state)
                        .withTooltip((status) -> Tooltip.create(lockToSend
                                ? localized("option", "key.mode.forced.tooltip",
                                KeybindUtil.localizeSendMode(TriState.State.ONE),
                                KeybindUtil.localizeConflict(QuadState.State.THREE))
                                : KeybindUtil.localizeSendModeTooltip(status)))
                        .create(x + width - buttonWidth, 0, modeButtonWidth, height,
                                localized("option", "key.mode"),
                                (button, status) -> {
                                    cmdKey.sendStrategy.state = status;
                                    listWidget.reload();
                                });
                if (lockToSend) modeButton.active = false;
                elements.add(modeButton);

                // Delay field or index button
                if (cmdKey.sendStrategy.state.equals(TriState.State.ZERO) || lockToSend) {
                    EditBox delayField = new EditBox(Minecraft.getInstance().font,
                            x + width - delayFieldWidth, 0, delayFieldWidth, height,
                            Component.empty());
                    delayField.setMaxLength(5);
                    delayField.setValue(String.valueOf(cmdKey.spaceTicks));
                    delayField.setResponder((val) -> {
                        try {
                            int ticks = Integer.parseInt(val);
                            if (ticks < -1) ticks = -1;
                            int oldTicks = cmdKey.spaceTicks;
                            cmdKey.spaceTicks = ticks;
                            if ((ticks == -1 && ticks != oldTicks) || (ticks != -1 && oldTicks == -1)) {
                                listWidget.reload();
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    });
                    delayField.setTooltip(Tooltip.create(
                            localized("option", "key.delay.tooltip")));
                    elements.add(delayField);
                }
                else if (cmdKey.sendStrategy.state.equals(TriState.State.TWO)) {
                    List<Integer> values = new ArrayList<>();
                    for (int i = 0; i < cmdKey.getMessages().size(); i++) values.add(i);
                    if (values.isEmpty()) values.add(0);
                    if (cmdKey.cycleIndex > values.getLast()) cmdKey.cycleIndex = 0;
                    elements.add(CycleButton.<Integer>builder(
                                    (status) -> Component.literal(status.toString()))
                            .withValues(values)
                            .withInitialValue(cmdKey.cycleIndex)
                            .displayOnlyValue()
                            .withTooltip((status) -> Tooltip.create(
                                    localized("option", "key.cycle_index.tooltip")))
                            .create(x + width - listWidget.smallButtonWidth, 0,
                                    listWidget.smallButtonWidth, height, Component.empty(),
                                    (button, status) -> cmdKey.cycleIndex = status));
                }
            }
        }

        private static class MessageEntry extends Entry {
            MessageEntry(int x, int width, int height, CommandKeyOptionsList listWidget,
                         CommandKey cmdKey, int index) {
                super();

                boolean showDelayField = (cmdKey.conflictStrategy.state == QuadState.State.THREE
                        || cmdKey.sendStrategy.state == TriState.State.ZERO)
                        && cmdKey.spaceTicks == -1;
                int delayFieldWidth = Minecraft.getInstance().font.width("0000000");
                int msgFieldWidth = width - listWidget.smallButtonWidth * 2 - SPACING * 2
                        - (showDelayField ? delayFieldWidth + SPACING : 0);
                Message msg = cmdKey.getMessages().get(index);

                // Drag reorder button
                elements.add(Button.builder(Component.literal("\u2191\u2193"),
                                (button) -> {
                                    this.setDragging(true);
                                    listWidget.dragSourceSlot = listWidget.children().indexOf(this);
                                })
                        .pos(x, 0)
                        .size(listWidget.smallButtonWidth, height)
                        .build());

                // Response field
                MultiLineEditBox messageField = new MultiLineEditBox(Minecraft.getInstance().font,
                        x + listWidget.smallButtonWidth + SPACING, 0, msgFieldWidth, height * 2,
                        Component.empty(), Component.empty());
                messageField.setCharacterLimit(256);
                messageField.setValue(msg.string);
                messageField.setValueListener((val) -> msg.string = val.strip());
                elements.add(messageField);

                // Delay field
                if (showDelayField) {
                    EditBox delayField = new EditBox(Minecraft.getInstance().font,
                            x + listWidget.smallButtonWidth + msgFieldWidth + SPACING * 2, 0,
                            delayFieldWidth, height, Component.empty());
                    delayField.setTooltip(Tooltip.create(
                            localized("option", "key.delay.individual.tooltip")));
                    delayField.setTooltipDelay(Duration.ofMillis(500));
                    delayField.setMaxLength(5);
                    delayField.setValue(String.valueOf(msg.delayTicks));
                    delayField.setResponder((val) -> {
                        try {
                            int delay = Integer.parseInt(val.strip());
                            if (delay < 0) throw new NumberFormatException();
                            msg.delayTicks = delay;
                            delayField.setTextColor(16777215);
                        } catch (NumberFormatException ignored) {
                            delayField.setTextColor(16711680);
                        }
                    });
                    elements.add(delayField);
                }

                // Delete button
                elements.add(Button.builder(Component.literal("\u274C")
                                        .withStyle(ChatFormatting.RED),
                                (button) -> {
                                    cmdKey.removeMessage(index);
                                    listWidget.reload();
                                })
                        .pos(x + width - listWidget.smallButtonWidth, 0)
                        .size(listWidget.smallButtonWidth, height)
                        .build());
            }
        }
    }
}
