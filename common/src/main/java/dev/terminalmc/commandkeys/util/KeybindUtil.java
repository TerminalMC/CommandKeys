package dev.terminalmc.commandkeys.util;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.config.Macro;
import dev.terminalmc.commandkeys.config.Profile;
import dev.terminalmc.commandkeys.mixin.accessor.KeyMappingAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static dev.terminalmc.commandkeys.CommandKeys.profile;
import static dev.terminalmc.commandkeys.config.Macro.ConflictStrategy.*;
import static dev.terminalmc.commandkeys.config.Macro.SendMode.*;
import static dev.terminalmc.commandkeys.util.Localization.localized;

public class KeybindUtil {

    /**
     * @return the number of operations to cancel.
     * 0 -> None.
     * 1 -> KeyboardHandler#charTyped.
     * 2 -> KeyboardHandler#charTyped and KeyMapping#click.
     */
    public static int handleKey(InputConstants.Key key) {
        int cancel = 0;

        if (Minecraft.getInstance().screen == null && profile().keyMacroMap.containsKey(key)) {
            long window = Minecraft.getInstance().getWindow().getWindow();
            boolean canActivateLimited = false;

            // Get all macros matching the pressed key
            Collection<Macro> allMacros = profile().keyMacroMap.get(key);

            // Preference limited macros, so check if any can be activated
            for (Macro macro : allMacros) {
                if (
                        !macro.getConflictStrategy().equals(AVOID)
                        && !macro.getLimitKey().equals(InputConstants.UNKNOWN)
                        && InputConstants.isKeyDown(window, macro.getLimitKey().getValue())
                ) {
                    canActivateLimited = true;
                    break;
                }
            }

            List<Macro> activeMacros = new ArrayList<>();
            if (canActivateLimited) {
                // Only activate limited macros
                for (Macro macro : allMacros) {
                    if (
                            !macro.getConflictStrategy().equals(AVOID)
                            && !macro.getLimitKey().equals(InputConstants.UNKNOWN)
                            && InputConstants.isKeyDown(window, macro.getLimitKey().getValue())
                    ) {
                        activeMacros.add(macro);
                    }
                }
            }
            else {
                // Only activate non-limited macros
                for (Macro macro : allMacros) {
                    if (
                            !macro.getConflictStrategy().equals(AVOID)
                            && macro.getLimitKey().equals(InputConstants.UNKNOWN)
                    ) {

                        activeMacros.add(macro);
                    }
                }
            }

            for (Macro macro : activeMacros) {
                boolean send = true;
                switch(macro.getConflictStrategy()) {
                    case SUBMIT -> send = getConflict(key) == null;
                    case VETO -> cancel = 2;
                }
                if (send) {
                    macro.trigger();
                    if (cancel == 0 && macro.getSendMode().equals(TYPE)) cancel = 1;
                }
            }
        }

        return cancel;
    }

    public static @Nullable KeyMapping getConflict(InputConstants.Key key) {
        for (KeyMapping keyMapping : Minecraft.getInstance().options.keyMappings) {
            if (((KeyMappingAccessor)keyMapping).getKey().equals(key)) {
                return keyMapping;
            }
        }
        return null;
    }

    /**
     * @return an array of three {@link MutableComponent} instances, where the
     * first is a basic label with only the names of the bound keys, the second
     * is the same label but with colored brackets depending on conflict levels,
     * and the third is a tooltip with conflict info (if any).
     */
    public static MutableComponent[] getKeybindInfo(Macro macro) {
        MutableComponent conflictLabel;
        MutableComponent tooltip = Component.empty();
        boolean[] conflict = new boolean[]{false, false}; // Internal, Minecraft
        boolean checkMcKeys = !macro.getConflictStrategy().equals(AVOID);

        // Check conflicts and add info (if any) to tooltip
        checkConflict(macro.getLimitKey(), macro.profile, conflict, tooltip, checkMcKeys);
        checkConflict(macro.getKey(), macro.profile, conflict, tooltip, checkMcKeys);

        // Get the keybind names for the basic label
        MutableComponent label = macro.getLimitKey().equals(InputConstants.UNKNOWN)
                ? macro.getKey().getDisplayName().copy()
                : macro.getLimitKey().getDisplayName().copy().append(" + ")
                        .append(macro.getKey().getDisplayName());

        if (conflict[1]) {
            // There is a conflict with a Minecraft keybind, so apply red
            // brackets and add the current conflict strategy to the tooltip
            conflictLabel = Component.literal("[ ")
                    .append(label.withStyle(ChatFormatting.WHITE))
                    .append(" ]").withStyle(ChatFormatting.RED);
            tooltip.append("\n");
            tooltip.append(localized("option", "key.bind.tooltip.conflict_strategy",
                    localizeStrat(macro.getConflictStrategy())));
        }
        else if (conflict[0]) {
            // There is a conflict with another macro, so apply orange brackets
            conflictLabel = Component.literal("[ ")
                    .append(label.withStyle(ChatFormatting.WHITE))
                    .append(" ]").withStyle(ChatFormatting.GOLD);
        }
        else {
            // No conflict, so we use the plain label
            conflictLabel = label;
        }

        return new MutableComponent[]{label, conflictLabel, tooltip};
    }

    /**
     * Checks {@code key} against the keys used by other {@link Macro}
     * instances, and optionally against Minecraft keybinds, updating
     * {@code conflict} and {@code tooltip} accordingly.
     */
    public static void checkConflict(InputConstants.Key key, Profile profile,
                                     boolean[] conflict, MutableComponent tooltip,
                                     boolean checkMcKeys) {
        if (!key.equals(InputConstants.UNKNOWN)) {
            if (profile.keyMacroMap.get(key).size() > 1) {
                tooltip.append(localized("option", "key.bind.tooltip.conflict.internal",
                                key.getDisplayName().copy().withStyle(ChatFormatting.GOLD)))
                        .withStyle(ChatFormatting.WHITE);
                conflict[0] = true;
            }
            if (checkMcKeys) {
                KeyMapping conflictingMcKey = KeybindUtil.getConflict(key);
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

    public static Component localizeStrat(Macro.ConflictStrategy strategy) {
        return localized("option", "key.conflict."
                + strategy.toString().toLowerCase(Locale.ROOT))
                .withStyle(switch(strategy) {
                    case SUBMIT -> ChatFormatting.GREEN;
                    case ASSERT -> ChatFormatting.GOLD;
                    case VETO -> ChatFormatting.RED;
                    case AVOID -> ChatFormatting.DARK_AQUA;
                });
    }

    public static Component localizeStratTooltip(Macro.ConflictStrategy strategy) {
        return localized("option", "key.conflict."
                + strategy.toString().toLowerCase(Locale.ROOT) + ".tooltip");
    }

    public static Component localizeMode(Macro.SendMode mode) {
        return localized("option", "key.mode."
                + mode.toString().toLowerCase(Locale.ROOT))
                .withStyle(switch(mode) {
                    case SEND -> ChatFormatting.GREEN;
                    case TYPE -> ChatFormatting.GOLD;
                    case CYCLE -> ChatFormatting.DARK_AQUA;
                    case RANDOM -> ChatFormatting.LIGHT_PURPLE;
                    case REPEAT -> ChatFormatting.RED;
                });
    }

    public static Component localizeModeTooltip(Macro.SendMode mode) {
        return localized("option", "key.mode."
                + mode.toString().toLowerCase(Locale.ROOT) + ".tooltip");
    }
}
