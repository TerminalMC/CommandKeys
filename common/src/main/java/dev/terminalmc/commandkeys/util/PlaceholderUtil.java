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

package dev.terminalmc.commandkeys.util;

import com.mojang.datafixers.util.Pair;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.mixin.accessor.ChatComponentAccessor;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PlaceholderUtil {

    private static int faults;
    private static @Nullable BlockPos playerBlockPos;
    private static @Nullable BlockPos lookBlockPos;
    private static @Nullable Vec3 lookAngle;
    private static @Nullable String pmSenderName;

    private static final SimplePlaceholder[] SIMPLE_PLACEHOLDERS = {
            new SimplePlaceholder("%lastsent%", PlaceholderUtil::getLastMessage),
            new SimplePlaceholder("%lastcmd%", PlaceholderUtil::getLastCommand),
            new SimplePlaceholder("%clipboard%", () -> getClipboard(null)),
            new SimplePlaceholder("%myname%", PlaceholderUtil::getPlayerName),
            new SimplePlaceholder("%pmsender%", PlaceholderUtil::getPmSenderName),
            new SimplePlaceholder("%pos%", () -> getPlayerBlockPos(new String[]{"0", "0"})),
            new SimplePlaceholder("%x%", () -> getPlayerBlockX(new String[]{"0"})),
            new SimplePlaceholder("%y%", () -> getPlayerBlockY(new String[]{"0"})),
            new SimplePlaceholder("%z%", () -> getPlayerBlockZ(new String[]{"0"})),
            new SimplePlaceholder("%lpos%", () -> getLookBlockPos(new String[]{"0", "0"})),
            new SimplePlaceholder("%lx%", () -> getLookBlockX(new String[]{"0"})),
            new SimplePlaceholder("%ly%", () -> getLookBlockY(new String[]{"0"})),
            new SimplePlaceholder("%lz%", () -> getLookBlockZ(new String[]{"0"})),
    };

    private static final Placeholder[] REGEX_PLACEHOLDERS = {
            new Placeholder(Pattern.compile("%#(.*)%"), 1, PlaceholderUtil::getRecentChat),
            new Placeholder(Pattern.compile("%clipboard#(.*)%"), 1, PlaceholderUtil::getClipboard),
            new Placeholder(Pattern.compile("%pos([FBLR])(\\d+)%"), 2, PlaceholderUtil::getPlayerBlockPos),
            new Placeholder(Pattern.compile("%x([+-]\\d+)%"), 1, PlaceholderUtil::getPlayerBlockX),
            new Placeholder(Pattern.compile("%y([+-]\\d+)%"), 1, PlaceholderUtil::getPlayerBlockY),
            new Placeholder(Pattern.compile("%z([+-]\\d+)%"), 1, PlaceholderUtil::getPlayerBlockZ),
            new Placeholder(Pattern.compile("%lpos([FBLR])(\\d+)%"), 2, PlaceholderUtil::getLookBlockPos),
            new Placeholder(Pattern.compile("%lx([+-]\\d+)%"), 1, PlaceholderUtil::getLookBlockX),
            new Placeholder(Pattern.compile("%ly([+-]\\d+)%"), 1, PlaceholderUtil::getLookBlockY),
            new Placeholder(Pattern.compile("%lz([+-]\\d+)%"), 1, PlaceholderUtil::getLookBlockZ),
    };

    /**
     * Breaks if player is not in-game. Does not self-check for performance
     * reasons, but expects caller to validate.
     */
    public static Pair<String,Integer> replace(String message) {
        if (!message.contains("%")) return new Pair<>(message, 0);
        reset();
        for (SimplePlaceholder p : SIMPLE_PLACEHOLDERS) message = p.process(message);
        for (Placeholder p : REGEX_PLACEHOLDERS) message = p.process(message);

        return new Pair<>(message, faults);
    }

    private static void reset() {
        faults = 0;
        playerBlockPos = null;
        lookBlockPos = null;
        lookAngle = null;
        pmSenderName = null;
    }
    
    private static String fault() {
        faults++;
        return "?";
    }

    private record SimplePlaceholder(String string, Supplier<String> supplier) {
        public String process(String message) {
            if (!message.contains(string)) return message;
            String replacement = supplier.get();
            return message.replaceAll(string, replacement);
        }
    }

    private record Placeholder(Pattern pattern, int groups, Function<String[], String> operator) {
        public String process(String message) {
            Matcher matcher = pattern.matcher(message);
            while (true) {
                if (!matcher.find()) return message;
                String[] args = new String[groups];
                for (int i = 0; i < args.length; i++) args[i] = matcher.group(i + 1);
                message = matcher.replaceFirst(operator.apply(args));
            }
        }
    }

    // Incoming message

    private static String getRecentChat(@NotNull String[] pattern) {
        try {
            Pattern regex = Pattern.compile(pattern[0]);

            int i = 0;
            for (GuiMessage guiMsg : ((ChatComponentAccessor)
                    Minecraft.getInstance().gui.getChat()).getAllMessages()) {
                if (++i > 50) break;

                Matcher matcher = regex.matcher(guiMsg.content().getString());
                if (matcher.find()) {
                    try {
                        return matcher.group(1);
                    } catch (IndexOutOfBoundsException e) {
                        CommandKeys.LOG.error("Recent chat placeholder failed: Group 1 not available: " + e);
                        return fault();
                    }
                }
            }

            CommandKeys.LOG.warn("Recent chat placeholder failed: No message found: Checked " + i);
        } catch (PatternSyntaxException e) {
            CommandKeys.LOG.error("Recent chat placeholder failed: Invalid regex: " + e);
        }

        return fault();
    }

    // Clipboard

    private static String getClipboard(@Nullable String[] pattern) {
        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clipboard.isEmpty()) {
            CommandKeys.LOG.warn("Clipboard placeholder failed: No data");
            return fault();
        }
        if (pattern != null) {
            try {
                if (!Pattern.compile(pattern[0]).matcher(clipboard).find()) {
                    CommandKeys.LOG.warn("Clipboard placeholder failed: Non-matching regex");
                    return fault();
                }
            } catch (PatternSyntaxException e) {
                CommandKeys.LOG.warn("Clipboard placeholder failed: Invalid regex: " + e);
                return fault();
            }
        }
        return clipboard;
    }

    // Message history

    private static String getLastMessage() {
        String lastMsg = Minecraft.getInstance().gui.getChat().getRecentChat().peekLast();
        if (lastMsg == null) return fault();
        return lastMsg;
    }

    private static String getLastCommand() {
        if (Minecraft.getInstance().commandHistory().history() instanceof ArrayListDeque<String> deque) {
            String lastCmd = deque.peekLast();
            if (lastCmd != null) return lastCmd;
        } else {
            CommandKeys.LOG.error("Command history not ArrayListDeque");
        }
        return fault();
    }

    // Player name

    private static String getPlayerName() {
        return Minecraft.getInstance().player.getName().getString();
    }

    // Incoming private message sender

    private static String getPmSenderName() {
        if (pmSenderName != null) return pmSenderName;
        int i = 0;
        for (GuiMessage guiMsg : ((ChatComponentAccessor)
                Minecraft.getInstance().gui.getChat()).getAllMessages()) {
            if (++i > 50) break;
            Component msg = guiMsg.content();
            if (msg.getContents() instanceof TranslatableContents tc
                    && tc.getKey().contains("commands.message.display.incoming")) {
                pmSenderName = ((MutableComponent)tc.getArgs()[0]).getString();
                break;
            }
        }
        if (pmSenderName == null) {
            CommandKeys.LOG.warn("PmSenderName placeholder failed: No message found: Checked " + i);
            return fault();
        }
        return pmSenderName;
    }

    // Player position

    private static BlockPos updatePlayerBlockPos() {
        if (playerBlockPos == null)
            playerBlockPos = Minecraft.getInstance().player.blockPosition();
        return playerBlockPos;
    }

    private static BlockPos updateLookBlockPos() {
        // Note: ProjectileUtil.getEntityHitResult for entities
        if (lookBlockPos == null) {
            Minecraft mc = Minecraft.getInstance();
            // Distance is arbitrary but will do for now
            HitResult result = mc.player.pick(Math.max(384, 
                    (mc.levelRenderer.getLastViewDistance() + 1D) * 16), 0.0F, false);
            if (result.getType().equals(HitResult.Type.BLOCK)) {
                lookBlockPos = ((BlockHitResult)result).getBlockPos();
            }
        }
        return lookBlockPos;
    }

    private static Vec3 updateLookAngle() {
        if (lookAngle == null)
            lookAngle = Minecraft.getInstance().player.getLookAngle();
        return lookAngle;
    }

    private static String getPlayerBlockPos(String[] args) {
        if (updatePlayerBlockPos() == null || updateLookAngle() == null) return fault();
        int offset = Integer.parseInt(args[1]);
        Vec3 playerPos = playerBlockPos.getBottomCenter();
        if (offset != 0) playerPos = offsetCardinalDirection(
                playerPos, lookAngle, args[0], offset);
        return String.format("%d %d %d", Mth.floor(playerPos.x),
                Mth.floor(playerPos.y), Mth.floor(playerPos.z));
    }

    private static String getPlayerBlockX(String[] offset) {
        if (updatePlayerBlockPos() == null) return fault();
        return String.valueOf(Mth.floor(playerBlockPos.getX()) + Integer.parseInt(offset[0]));
    }

    private static String getPlayerBlockY(String[] offset) {
        if (updatePlayerBlockPos() == null) return fault();
        return String.valueOf(Mth.floor(playerBlockPos.getY()) + Integer.parseInt(offset[0]));
    }

    private static String getPlayerBlockZ(String[] offset) {
        if (updatePlayerBlockPos() == null) return fault();
        return String.valueOf(Mth.floor(playerBlockPos.getZ()) + Integer.parseInt(offset[0]));
    }

    private static String getLookBlockPos(String[] args) {
        if (updateLookBlockPos() == null || updateLookAngle() == null) return fault();
        int offset = Integer.parseInt(args[1]);
        Vec3 playerPos = lookBlockPos.getBottomCenter();
        if (offset != 0) playerPos = offsetCardinalDirection(
                playerPos, lookAngle, args[0], offset);
        return String.format("%d %d %d", Mth.floor(playerPos.x),
                Mth.floor(playerPos.y), Mth.floor(playerPos.z));
    }

    private static String getLookBlockX(String[] offset) {
        if (updateLookBlockPos() == null) return fault();
        return String.valueOf(Mth.floor(lookBlockPos.getX()) + Integer.parseInt(offset[0]));
    }

    private static String getLookBlockY(String[] offset) {
        if (updateLookBlockPos() == null) return fault();
        return String.valueOf(Mth.floor(lookBlockPos.getY()) + Integer.parseInt(offset[0]));
    }

    private static String getLookBlockZ(String[] offset) {
        if (updateLookBlockPos() == null) return fault();
        return String.valueOf(Mth.floor(lookBlockPos.getZ()) + Integer.parseInt(offset[0]));
    }

    // Util

    private static Vec3 offsetCardinalDirection(
            Vec3 pos, Vec3 facingAngle, String offsetDir, int offset) {
        if (Math.abs(facingAngle.x) >= Math.abs(facingAngle.z)) {
            if (facingAngle.x >= 0) { // East
                return switch(offsetDir) {
                    case "F" -> new Vec3(pos.x + offset, pos.y, pos.z);
                    case "B" -> new Vec3(pos.x - offset, pos.y, pos.z);
                    case "L" -> new Vec3(pos.x, pos.y, pos.z - offset);
                    case "R" -> new Vec3(pos.x, pos.y, pos.z + offset);
                    default -> throw new IllegalArgumentException("Disallowed value " + offsetDir);
                };
            } else {
                return switch(offsetDir) { // West
                    case "F" -> new Vec3(pos.x - offset, pos.y, pos.z);
                    case "B" -> new Vec3(pos.x + offset, pos.y, pos.z);
                    case "L" -> new Vec3(pos.x, pos.y, pos.z + offset);
                    case "R" -> new Vec3(pos.x, pos.y, pos.z - offset);
                    default -> throw new IllegalArgumentException("Disallowed value " + offsetDir);
                };
            }
        } else {
            if (facingAngle.z >= 0) {
                return switch(offsetDir) { // South
                    case "F" -> new Vec3(pos.x, pos.y, pos.z + offset);
                    case "B" -> new Vec3(pos.x, pos.y, pos.z - offset);
                    case "L" -> new Vec3(pos.x + offset, pos.y, pos.z);
                    case "R" -> new Vec3(pos.x - offset, pos.y, pos.z);
                    default -> throw new IllegalArgumentException("Disallowed value " + offsetDir);
                };
            } else {
                return switch(offsetDir) { // North
                    case "F" -> new Vec3(pos.x, pos.y, pos.z - offset);
                    case "B" -> new Vec3(pos.x, pos.y, pos.z + offset);
                    case "L" -> new Vec3(pos.x - offset, pos.y, pos.z);
                    case "R" -> new Vec3(pos.x + offset, pos.y, pos.z);
                    default -> throw new IllegalArgumentException("Disallowed value " + offsetDir);
                };
            }
        }
    }
}
