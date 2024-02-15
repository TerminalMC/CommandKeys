package notryken.commandkeys.gui.component.listwidget;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import notryken.commandkeys.config.CommandKey;
import notryken.commandkeys.config.TriState;

import java.util.ArrayList;

public class CommandKeyListWidget extends ConfigListWidget {
    private final CommandKey commandKey;

    public CommandKeyListWidget(Minecraft minecraft, int width, int height, int top, int bottom,
                                int itemHeight, int entryRelX, int entryWidth, int entryHeight,
                                int scrollWidth, CommandKey commandKey) {
        super(minecraft, width, height, top, bottom, itemHeight, entryRelX,
                entryWidth, entryHeight, scrollWidth);
        this.commandKey = commandKey;

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Command Mono-Key Options \u2139"),
                Tooltip.create(Component.literal("These options only apply to the " +
                        "selected command key.")), 500));

        addEntry(new Entry.ConflictStrategyEntry(entryX, entryWidth, entryHeight, this, commandKey));
        addEntry(new Entry.SendTypeEntry(entryX, entryWidth, entryHeight, commandKey));
        addEntry(new Entry.CycleToggleEntry(entryX, entryWidth, entryHeight, this, commandKey));
        if (commandKey.cycle) addEntry(new Entry.CycleIndexEntry(entryX, entryWidth, entryHeight, commandKey));
    }

    @Override
    public ConfigListWidget resize(int width, int height, int top, int bottom,
                                   int itemHeight, double scrollAmount) {
        CommandKeyListWidget newListWidget = new CommandKeyListWidget(
                minecraft, width, height, top, bottom, itemHeight,
                entryRelX, entryWidth, entryHeight, scrollWidth, commandKey);
        newListWidget.setScrollAmount(scrollAmount);
        return newListWidget;
    }

    @Override
    public boolean keyPressed(InputConstants.Key key) {
        return false;
    }

    @Override
    public boolean keyReleased(InputConstants.Key key) {
        return false;
    }

    @Override
    public boolean mouseClicked(InputConstants.Key key) {
        return false;
    }

    @Override
    public boolean mouseReleased(InputConstants.Key key) {
        return false;
    }

    private abstract static class Entry extends ConfigListWidget.Entry {

        private static class ConflictStrategyEntry extends Entry {
            ConflictStrategyEntry(int x, int width, int height, CommandKeyListWidget listWidget,
                                  CommandKey commandKey) {
                super();
                CycleButton<TriState.State> cycleButton = CycleButton.<TriState.State>builder(
                        (status) -> switch(status) {
                            case ZERO -> Component.literal("Submissive").withStyle(ChatFormatting.GREEN);
                            case ONE -> Component.literal("Assertive").withStyle(ChatFormatting.GOLD);
                            case TWO -> Component.literal("Aggressive").withStyle(ChatFormatting.RED);
                        })
                        .withValues(TriState.State.values())
                        .withInitialValue(commandKey.conflictStrategy.state)
                        .withTooltip((status) -> Tooltip.create(Component.literal(switch(status) {
                            case ZERO -> "If the key is already used by Minecraft, this keybind will be cancelled.";
                            case ONE -> "If the key is already used by Minecraft, this keybind will be activated " +
                                    "first, then the other keybind.";
                            case TWO -> "If the key is already used by Minecraft, the other keybind will be " +
                                    "cancelled.\nNote: Some keys (including movement and sneak) cannot be cancelled.";
                        })))
                        .create(x, 0, width, height, Component.literal("Conflict Strategy"),
                                (button, status) -> {
                                    commandKey.conflictStrategy.state = status;
                                    listWidget.reload();
                                });
                cycleButton.setTooltipDelay(500);
                elements.add(cycleButton);
            }
        }

        private static class SendTypeEntry extends Entry {
            SendTypeEntry(int x, int width, int height, CommandKey commandKey) {
                super();
                CycleButton<Boolean> cycleButton = CycleButton.booleanBuilder(
                        Component.literal("Send").withStyle(ChatFormatting.GREEN),
                                Component.literal("Type").withStyle(ChatFormatting.GOLD))
                        .withInitialValue(commandKey.fullSend)
                        .withTooltip((status) -> Tooltip.create(Component.literal(status ?
                                "Messages will be sent when the key is pressed." :
                                "The first message will be entered into the chat box, but not sent.")))
                        .create(x, 0, width, height, Component.literal("Send Behavior"),
                                (button, status) -> commandKey.fullSend = status);
                cycleButton.setTooltipDelay(500);
                elements.add(cycleButton);
            }
        }

        private static class CycleToggleEntry extends Entry {
            CycleToggleEntry(int x, int width, int height, CommandKeyListWidget listWidget,
                             CommandKey commandKey) {
                super();
                CycleButton<Boolean> cycleButton = CycleButton.booleanBuilder(
                        Component.translatable("options.on").withStyle(ChatFormatting.GREEN),
                                Component.translatable("options.off").withStyle(ChatFormatting.RED))
                        .withInitialValue(commandKey.cycle)
                        .withTooltip((status) -> Tooltip.create(Component.literal(status ?
                                "Messages will be cycled through as you repeatedly press the key. " +
                                        "To send multiple messages in one keypress, separate them " +
                                        "with a pair of commas e.g. /lobby,,/nick" :
                                "All messages will be sent when the key is pressed.")))
                        .create(x, 0, width, height, Component.literal("Cycle Messages"),
                                (button, status) -> {
                                    commandKey.cycle = status;
                                    listWidget.reload();
                                });
                cycleButton.setTooltipDelay(500);
                elements.add(cycleButton);
            }
        }

        private static class CycleIndexEntry extends Entry {
            CycleIndexEntry(int x, int width, int height, CommandKey commandKey) {
                super();

                ArrayList<Integer> values = new ArrayList<>();
                for (int i = 0; i < commandKey.messages.size(); i++) values.add(i);
                if (values.isEmpty()) values.add(0);
                if (commandKey.nextIndex > commandKey.messages.size() - 1) commandKey.nextIndex = 0;

                CycleButton<Integer> cycleButton = CycleButton.<Integer>builder(
                                (status) -> Component.literal(status.toString()).withStyle(ChatFormatting.GOLD))
                        .withValues(values)
                        .withInitialValue(commandKey.nextIndex)
                        .withTooltip((status) -> Tooltip.create(
                                Component.literal("The current position in the cycle.")))
                        .create(x, 0, width, height, Component.literal("Cycle Value"),
                                (button, status) -> commandKey.nextIndex = status);
                cycleButton.setTooltipDelay(500);
                elements.add(cycleButton);
            }
        }
    }
}
