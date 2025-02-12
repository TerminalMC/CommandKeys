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

package dev.terminalmc.commandkeys.config;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.util.JsonUtil;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Consists of behavioral options, a primary and alternate {@link Keybind}, and 
 * a list of {@link Message} instances.
 */
public class Macro {
    public static final int VERSION = 5;
    public final int version = VERSION;

    public static final Random RANDOM = new Random();
    
    boolean addToHistory;
    public static final boolean addToHistoryDefault = false;
    transient boolean addToHistoryStatus;
    
    boolean showHudMessage;
    public static final boolean showHudMessageDefault = false;
    transient boolean showHudMessageStatus;
    
    boolean resumeRepeating;
    public static final boolean resumeRepeatingDefault = false;
    transient boolean resumeRepeatingStatus;
    
    boolean useRatelimit;
    public static final boolean useRatelimitDefault = false;
    transient boolean useRatelimitStatus;

    ConflictStrategy conflictStrategy;
    public static final ConflictStrategy conflictStrategyDefault = ConflictStrategy.SUBMIT;
    public enum ConflictStrategy {
        SUBMIT,
        ASSERT,
        VETO,
        AVOID,
    }
    
    SendMode sendMode;
    public static final SendMode sendModeDefault = SendMode.SEND;
    public enum SendMode {
        SEND,
        TYPE,
        CYCLE,
        RANDOM,
        REPEAT,
    }

    /**
     * Standard delay between messages when sending.
     */
    public int spaceTicks;
    public static final int spaceTicksDefault = 0;
    
    /**
     * Index of next message forwards when cycling.
     */
    public transient int cycleIndex;
    public static final int cycleIndexDefault = 0;

    /**
     * Primary keybind used for activation.
     */
    Keybind keybind;
    public static final Supplier<Keybind> keybindDefault = Keybind::new;

    /**
     * Alternate keybind used for activation of special functions.
     */
    Keybind altKeybind;
    public static final Supplier<Keybind> altKeybindDefault = Keybind::new;
    
    final List<Message> messages;
    public static final Supplier<List<Message>> messagesDefault = ArrayList::new;

    /**
     * Creates a default empty instance.
     */
    public Macro() {
        this(
                addToHistoryDefault,
                showHudMessageDefault,
                resumeRepeatingDefault,
                useRatelimitDefault,
                Config.get().defaultConflictStrategy,
                Config.get().defaultSendMode,
                spaceTicksDefault,
                cycleIndexDefault,
                keybindDefault.get(),
                altKeybindDefault.get(),
                messagesDefault.get()
        );
    }

    /**
     * Not validated, only for use by self-validating deserializer.
     */
    Macro(
            boolean addToHistory,
            boolean showHudMessage,
            boolean resumeRepeating,
            boolean useRatelimit,
            ConflictStrategy conflictStrategy,
            SendMode sendMode,
            int spaceTicks,
            int cycleIndex,
            Keybind keybind,
            Keybind altKeybind,
            List<Message> messages
    ) {
        this.addToHistory = addToHistory;
        this.showHudMessage = showHudMessage;
        this.resumeRepeating = resumeRepeating;
        this.useRatelimit = useRatelimit;
        this.conflictStrategy = conflictStrategy;
        this.sendMode = sendMode;
        this.spaceTicks = spaceTicks;
        this.cycleIndex = cycleIndex;
        this.keybind = keybind;
        this.altKeybind = altKeybind;
        this.messages = messages;
    }

    /**
     * Copy constructor.
     */
    Macro(Macro macro) {
        this.addToHistory = macro.addToHistory;
        this.addToHistoryStatus = macro.addToHistoryStatus;
        this.showHudMessage = macro.showHudMessage;
        this.showHudMessageStatus = macro.showHudMessageStatus;
        this.resumeRepeating = macro.resumeRepeating;
        this.resumeRepeatingStatus = macro.resumeRepeatingStatus;
        this.useRatelimit = macro.useRatelimit;
        this.useRatelimitStatus = macro.useRatelimitStatus;
        this.conflictStrategy = macro.conflictStrategy;
        this.sendMode = macro.sendMode;
        this.spaceTicks = macro.spaceTicks;
        this.cycleIndex = macro.cycleIndex;
        this.keybind = new Keybind(macro.keybind);
        this.altKeybind = new Keybind(macro.altKeybind);
        this.messages = macro.messages.stream().map(Message::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }
    
    // Control accessors

    public boolean getAddToHistory() {
        return addToHistory;
    }

    public boolean getAddToHistoryStatus() {
        return addToHistoryStatus;
    }

    public boolean getShowHudMessage() {
        return showHudMessage;
    }

    public boolean getShowHudMessageStatus() {
        return showHudMessageStatus;
    }

    public boolean getResumeRepeating() {
        return resumeRepeating;
    }

    public boolean getResumeRepeatingStatus() {
        return resumeRepeatingStatus;
    }

    public boolean getUseRatelimit() {
        return useRatelimit;
    }

    public boolean getUseRatelimitStatus() {
        return useRatelimitStatus;
    }

    public ConflictStrategy getStrategy() {
        return conflictStrategy;
    }

    public SendMode getMode() {
        return sendMode;
    }

    public Keybind getKeybind() {
        return keybind;
    }

    public Keybind getAltKeybind() {
        return altKeybind;
    }
    
    // Keybind utils

    /**
     * @return {@code true} if {@code keybind} belongs to and is in active use
     * by this macro, {@code false} otherwise.
     */
    public boolean usesKeybind(Keybind keybind) {
        return (keybind == this.keybind) || (keybind == this.altKeybind);
    }

    /**
     * @return {@code true} if this macro is using its alternate keybind, 
     * {@code false} otherwise.
     */
    public boolean usesAltKeybind() {
        return sendMode.equals(SendMode.CYCLE);
    }
    
    // Message management

    /**
     * @return an unmodifiable view of the messages list.
     */
    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void addMessage(Message message) {
        this.messages.add(message);
    }

    public void setMessage(int index, String str) {
        this.messages.get(index).string = str;
    }

    public void removeMessage(int index) {
        this.messages.remove(index);
    }

    /**
     * Moves the message at the source index to the destination index.
     * @param sourceIndex the index of the element to move.
     * @param destIndex the desired final index of the element.
     */
    public boolean moveMessage(int sourceIndex, int destIndex) {
        if (sourceIndex != destIndex) {
            messages.add(destIndex, messages.remove(sourceIndex));
            return true;
        }
        return false;
    }

    // Activation
    
    public void trigger(@Nullable Keybind trigger) {
        // Triggering a repeating macro stops it
        if (hasRepeating()) {
            stopRepeating();
            return;
        }

        // Otherwise, activate it
        switch(sendMode) {
            case SEND -> {
                // If using standard delay, doesn't apply to first
                boolean standardDelay = spaceTicks != 0;
                int totalDelay = standardDelay ? -spaceTicks : 0;
                // Send all messages with cumulative delays
                for (Message msg : messages) {
                    totalDelay += standardDelay ? spaceTicks : msg.delayTicks;
                    if (!msg.string.isBlank()) {
                        schedule(totalDelay, msg.string, addToHistoryStatus, showHudMessageStatus);
                    }
                }
            }
            case TYPE -> {
                // Type the first message
                if (!messages.isEmpty()) {
                    CommandKeys.type(messages.getFirst().string);
                }
            }
            case CYCLE -> {
                if (altKeybind.equals(trigger)) {
                    // Alt keybind cycles backwards
                    if (cycleIndex == 0) cycleIndex = messages.size() - 1;
                    else cycleIndex--;
                }
                else {
                    // Main keybind cycles forwards
                    if (++cycleIndex >= messages.size()) cycleIndex = 0;
                }
                // Split to allow multiple messages per press
                for (String str : messages.get(cycleIndex).string.split(",,")) {
                    // Blank messages are treated as spacers
                    if (!str.isBlank()) {
                        CommandKeys.send(str, addToHistoryStatus, showHudMessageStatus);
                    }
                }
            }
            case RANDOM -> {
                // Pick a random message and send it
                if (!messages.isEmpty()) {
                    Message msg = messages.get(RANDOM.nextInt(messages.size()));
                    if (!msg.string.isBlank()) {
                        schedule(msg.delayTicks, msg.string,
                                addToHistoryStatus, showHudMessageStatus);
                    }
                }
            }
            case REPEAT -> {
                // Schedule messages spaced by individual delays, repeating
                // every spaceTicks
                int totalDelay = 0;
                for (Message msg : messages) {
                    totalDelay += msg.delayTicks;
                    if (!msg.string.isBlank()) {
                        schedule(totalDelay, spaceTicks, msg.string, 
                                addToHistoryStatus, showHudMessageStatus);
                    }
                }
            }
        }
    }

    // Scheduling

    private transient final List<ScheduledMessage> scheduledMessages = new ArrayList<>();

    /**
     * Clears all scheduled messages, including repeating ones.
     */
    public void clearScheduled() {
        scheduledMessages.clear();
    }

    /**
     * @return {@code true} if any scheduled messages are set to repeat,
     * {@code false} otherwise.
     */
    public boolean hasRepeating() {
        for (ScheduledMessage msg : scheduledMessages) {
            if (msg.repeatDelay >= 0) return true;
        }
        return false;
    }

    /**
     * Removes all repeating messages from the schedule.
     */
    public void stopRepeating() {
        scheduledMessages.removeIf((msg) -> msg.repeatDelay >= 0);
    }

    /**
     * Schedules the message to send after {@code delay} ticks.
     */
    private void schedule(int delay, String message, boolean addToHistory, boolean showHudMessage) {
        schedule(delay, -1, message, addToHistory, showHudMessage);
    }

    /**
     * Schedules the message to send after {@code initialDelay} ticks, repeating
     * every {@code repeatDelay} ticks.
     */
    private void schedule(int initialDelay, int repeatDelay, String message,
                          boolean addToHistory, boolean showHudMessage) {
        scheduledMessages.add(new ScheduledMessage(initialDelay, repeatDelay, message,
                addToHistory, showHudMessage));
    }

    public void tick() {
        scheduledMessages.removeIf(ScheduledMessage::tick);
    }

    private static class ScheduledMessage {
        private int delay;
        final int repeatDelay;
        final String message;
        final boolean showHudMessage;
        final boolean addToHistory;

        public ScheduledMessage(int initialDelay, int repeatDelay, String message,
                                boolean showHudMessage, boolean addToHistory) {
            this.delay = initialDelay;
            this.repeatDelay = repeatDelay;
            this.message = message;
            this.showHudMessage = showHudMessage;
            this.addToHistory = addToHistory;
        }

        /**
         * @return {@code true} if this {@link ScheduledMessage} has finished
         * ticking, {@code false} otherwise.
         */
        private boolean tick() {
            if (--delay <= 0) {
                CommandKeys.send(message, showHudMessage, addToHistory);
                if (repeatDelay >= 0) delay = repeatDelay;
                else return true;
            }
            return false;
        }
    }

    // Validation

    Macro validate() {
        if (spaceTicks < 0) spaceTicks = 0;
        
        keybind.validate();
        altKeybind.validate();
        
        messages.forEach(Message::validate);
        cleanupMessages();
        
        return this;
    }

    void cleanupMessages() {
        messages.removeIf((msg) -> {
            // Never allow leading whitespace
            msg.string = msg.string.stripLeading();
            // Only allow trailing whitespace for TYPE mode
            if (!sendMode.equals(SendMode.TYPE)) {
                msg.string = msg.string.stripTrailing();
            }
            // Only allow blank messages for CYCLE mode (as spacers) and TYPE
            // mode (to open chat)
            return (msg.string.isBlank()
                    && !sendMode.equals(SendMode.CYCLE)
                    && !sendMode.equals(SendMode.TYPE));
        });
    }

    // Deserialization

    public static class Deserializer implements JsonDeserializer<Macro> {
        @Override
        public Macro deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            int version = obj.has("version") ? obj.get("version").getAsInt() : 0;
            boolean silent = version != VERSION;

            boolean addToHistory = JsonUtil.getOrDefault(obj, "addToHistory",
                    addToHistoryDefault, silent);

            boolean showHudMessage = JsonUtil.getOrDefault(obj, "showHudMessage",
                    showHudMessageDefault, silent);

            boolean resumeRepeating = JsonUtil.getOrDefault(obj, "resumeRepeating",
                    resumeRepeatingDefault, silent);

            boolean useRatelimit = JsonUtil.getOrDefault(obj, "useRatelimit",
                    useRatelimitDefault, silent);

            ConflictStrategy conflictStrategy = version >= 3 // Since 2.1.0-beta.2
                    ? JsonUtil.getOrDefault(obj, "conflictStrategy",
                    ConflictStrategy.class, conflictStrategyDefault, silent)
                    : getConflictStrategy(JsonUtil.getOrDefault(obj, "conflictStrategy",
                    "", true));

            SendMode sendMode = version >= 3 // Since 2.1.0-beta.2
                    ? JsonUtil.getOrDefault(obj, "sendMode",
                    SendMode.class, sendModeDefault, silent)
                    : getSendMode(JsonUtil.getOrDefault(obj, "sendMode",
                    "", true));

            int spaceTicks = JsonUtil.getOrDefault(obj, "spaceTicks",
                    spaceTicksDefault, silent);
            
            Keybind keybind = version >= 4 // Since 2.3.0-beta.1
                    ? JsonUtil.getOrDefault(ctx, obj, "keybind",
                    Keybind.class, new Keybind(), silent)
                    : new Keybind(
                            JsonUtil.getOrDefault(obj, "keyName",
                                    InputConstants.UNKNOWN, true),
                            JsonUtil.getOrDefault(obj, "limitKeyName",
                                    InputConstants.UNKNOWN, true)
                    ).validate();

            Keybind altKeybind = JsonUtil.getOrDefault(ctx, obj, "altKeybind",
                    Keybind.class, new Keybind(), silent);
            
            List<Message> messages = JsonUtil.getOrDefault(ctx, obj, "messages", 
                    Message.class, messagesDefault.get(), silent);

            return new Macro(
                    addToHistory,
                    showHudMessage,
                    resumeRepeating,
                    useRatelimit,
                    conflictStrategy,
                    sendMode,
                    spaceTicks,
                    0,
                    keybind,
                    altKeybind,
                    messages
            ).validate();
        }

        public static ConflictStrategy getConflictStrategy(String str) {
            return switch(str) {
                case "ZERO" -> ConflictStrategy.SUBMIT;
                case "ONE" -> ConflictStrategy.ASSERT;
                case "TWO" -> ConflictStrategy.VETO;
                case "THREE" -> ConflictStrategy.AVOID;
                default -> conflictStrategyDefault;
            };
        }

        public static SendMode getSendMode(String str) {
            return switch(str) {
                case "ZERO" -> SendMode.SEND;
                case "ONE" -> SendMode.TYPE;
                case "TWO" -> SendMode.CYCLE;
                default -> sendModeDefault;
            };
        }
    }
}
