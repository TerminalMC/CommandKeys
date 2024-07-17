package dev.terminalmc.commandkeys.util;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.config.CommandKey;
import dev.terminalmc.commandkeys.config.QuadState;
import dev.terminalmc.commandkeys.config.TriState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import static dev.terminalmc.commandkeys.util.Localization.localized;

public class KeybindUtil {
    public static MutableComponent[] getKeybindInfo(CommandKey cmdKey) {
        MutableComponent label;
        MutableComponent tooltip = Component.empty();

        // Check conflict and build tooltip
        boolean[] conflict = new boolean[]{false, false}; // Internal, Minecraft
        boolean checkMcConflict = !cmdKey.conflictStrategy.state.equals(QuadState.State.THREE);
        checkConflict(cmdKey, cmdKey.getLimitKey(), tooltip, conflict, checkMcConflict);
        checkConflict(cmdKey, cmdKey.getKey(), tooltip, conflict, checkMcConflict);

        MutableComponent keyDisplayName = cmdKey.getLimitKey().equals(InputConstants.UNKNOWN)
                ? cmdKey.getKey().getDisplayName().copy()
                : cmdKey.getLimitKey().getDisplayName().copy().append(" + ")
                .append(cmdKey.getKey().getDisplayName());

        if (conflict[1]) { // Conflict with a Minecraft keybind, red warning
            label = Component.literal("[ ")
                    .append(keyDisplayName.withStyle(ChatFormatting.WHITE))
                    .append(" ]").withStyle(ChatFormatting.RED);
            tooltip.append("\n");
            tooltip.append(localized("option", "key.bind.tooltip.conflict_strategy",
                    localizeConflict(cmdKey.conflictStrategy.state)));
        }
        else if (conflict[0]) { // Conflict with another CommandKey, orange warning
            label = Component.literal("[ ")
                    .append(keyDisplayName.withStyle(ChatFormatting.WHITE))
                    .append(" ]").withStyle(ChatFormatting.GOLD);
        }
        else {
            label = keyDisplayName;
        }

        return new MutableComponent[]{keyDisplayName, label, tooltip};
    }

    public static void checkConflict(CommandKey cmdKey, InputConstants.Key key,
                               MutableComponent tooltip, boolean[] conflict,
                               boolean checkMcConflict) {
        if (!key.equals(InputConstants.UNKNOWN)) {
            if (cmdKey.profile.commandKeyMap.get(key).size() > 1) {
                tooltip.append(localized("option", "key.bind.tooltip.conflict.internal",
                                key.getDisplayName().copy().withStyle(ChatFormatting.GOLD)))
                        .withStyle(ChatFormatting.WHITE);
                conflict[0] = true;
            }
            if (checkMcConflict) {
                KeyMapping conflictingMcKey = KeyUtil.getConflict(key);
                if (conflictingMcKey != null) {
                    if (conflict[0] || conflict[1]) tooltip.append("\n");
                    tooltip.append(localized("option", "key.bind.tooltip.conflict.external",
                                    key.getDisplayName().copy().withStyle(ChatFormatting.RED),
                                    Component.translatable(conflictingMcKey.getName())
                                            .withStyle(ChatFormatting.GRAY)))
                            .withStyle(ChatFormatting.WHITE);
                    conflict[1] = true;
                }
            }
        }
    }

    public static Component localizeConflict(QuadState.State state) {
        return switch(state) {
            case ZERO -> localized("option", "key.conflict.submit")
                    .withStyle(ChatFormatting.GREEN);
            case ONE ->localized("option", "key.conflict.assert")
                    .withStyle(ChatFormatting.GOLD);
            case TWO -> localized("option", "key.conflict.veto")
                    .withStyle(ChatFormatting.RED);
            case THREE -> localized("option", "key.conflict.avoid")
                    .withStyle(ChatFormatting.DARK_AQUA);
        };
    }

    public static Component localizeConflictTooltip(QuadState.State state) {
        return switch(state) {
            case ZERO -> localized("option", "key.conflict.submit.tooltip");
            case ONE ->localized("option", "key.conflict.assert.tooltip");
            case TWO -> localized("option", "key.conflict.veto.tooltip");
            case THREE -> localized("option", "key.conflict.avoid.tooltip");
        };
    }

    public static Component localizeSendMode(TriState.State state) {
        return switch(state) {
            case ZERO -> localized("option", "key.mode.send")
                    .withStyle(ChatFormatting.GREEN);
            case ONE -> localized("option", "key.mode.type")
                    .withStyle(ChatFormatting.GOLD);
            case TWO -> localized("option", "key.mode.cycle")
                    .withStyle(ChatFormatting.DARK_AQUA);
        };
    }

    public static Component localizeSendModeTooltip(TriState.State state) {
        return switch(state) {
            case ZERO -> localized("option", "key.mode.send.tooltip");
            case ONE -> localized("option", "key.mode.type.tooltip");
            case TWO -> localized("option", "key.mode.cycle.tooltip");
        };
    }
}
