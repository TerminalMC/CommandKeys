/*
 * Copyright 2024 TerminalMC
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
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Consists of behavioral options, a primary and alternate {@link Keybind}, and 
 * a list of {@link Message} instances.
 */
public class Macro {
    public final int version = 4;

    public static final Random RANDOM = new Random();

    boolean addToHistory;
    public transient boolean historyEnabled;
    boolean showHudMessage;
    public transient boolean hudMessageEnabled;
    
    public boolean ignoreRatelimit;

    ConflictStrategy conflictStrategy;
    public enum ConflictStrategy {
        SUBMIT,
        ASSERT,
        VETO,
        AVOID,
    }
    
    SendMode sendMode;
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
    /**
     * Index of next message forwards when cycling.
     */
    public transient int cycleIndex;

    Keybind keybind;
    Keybind altKeybind;

    final List<Message> messages;

    /**
     * Creates a default empty instance.
     */
    public Macro() {
        this.addToHistory = false;
        this.showHudMessage = false;
        this.ignoreRatelimit = false;
        this.conflictStrategy = Config.get().defaultConflictStrategy;
        this.sendMode = Config.get().defaultSendMode;
        this.spaceTicks = 0;
        this.cycleIndex = 0;
        this.keybind = new Keybind();
        this.altKeybind = new Keybind();
        this.messages = new ArrayList<>();
    }

    /**
     * Not validated, only for use by self-validating deserializer.
     */
    private Macro(boolean addToHistory, boolean showHudMessage, boolean ignoreRatelimit,
                  ConflictStrategy conflictStrategy, SendMode sendMode, int spaceTicks,
                  Keybind keybind, Keybind altKeybind, List<Message> messages) {
        this.addToHistory = addToHistory;
        this.showHudMessage = showHudMessage;
        this.ignoreRatelimit = ignoreRatelimit;
        this.conflictStrategy = conflictStrategy;
        this.sendMode = sendMode;
        this.spaceTicks = spaceTicks;
        this.cycleIndex = 0;
        this.keybind = keybind;
        this.altKeybind = altKeybind;
        this.messages = messages;
    }

    public boolean getAddToHistory() {
        return addToHistory;
    }

    public boolean getShowHudMessage() {
        return showHudMessage;
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

    /**
     * @return {@code true} if {@code keybind} belongs to and is in active use
     * by this macro, {@code false} otherwise.
     */
    public boolean usesKeybind(Keybind keybind) {
        return (keybind == this.keybind) || (usesAltKeybind() && keybind == this.altKeybind);
    }

    /**
     * @return {@code true} if this macro is using its alternate keybind, 
     * {@code false} otherwise.
     */
    public boolean usesAltKeybind() {
        return sendMode.equals(SendMode.CYCLE);
    }

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
    public void moveMessage(int sourceIndex, int destIndex) {
        if (sourceIndex != destIndex) {
            messages.add(destIndex, messages.remove(sourceIndex));
        }
    }

    // Activation

    public void trigger(@Nullable Keybind trigger) {
        if (hasRepeating()) {
            stopRepeating();
            return;
        }

        switch(sendMode) {
            case SEND -> {
                // If using standard delay, doesn't apply to first
                boolean standardDelay = spaceTicks != 0;
                int cumulativeDelay = standardDelay ? -spaceTicks : 0;
                for (Message msg : messages) {
                    cumulativeDelay += standardDelay ? spaceTicks : msg.delayTicks;
                    schedule(cumulativeDelay, -1, msg.string, 
                            historyEnabled, hudMessageEnabled);
                }
            }
            case TYPE -> {
                if (!messages.isEmpty()) {
                    CommandKeys.type(messages.get(0).string);
                }
            }
            case CYCLE -> {
                if (altKeybind.equals(trigger)) {
                    if (cycleIndex == 0) cycleIndex = messages.size() - 1;
                    else cycleIndex--;
                } else {
                    if (++cycleIndex >= messages.size()) cycleIndex = 0;
                }
                // Allow spacer blank messages, and multiple messages per press.
                for (String msg : messages.get(cycleIndex).string.split(",,")) {
                    if (!msg.isBlank()) {
                        CommandKeys.send(msg, historyEnabled, hudMessageEnabled);
                    }
                }
            }
            case RANDOM -> {
                if (!messages.isEmpty()) {
                    Message msg = messages.get(RANDOM.nextInt(messages.size()));
                    if (!msg.string.isBlank()) {
                        CommandKeys.send(msg.string, historyEnabled, hudMessageEnabled);
                    }
                }
            }
            case REPEAT -> {
                int cumulativeDelay = 0;
                for (Message msg : messages) {
                    cumulativeDelay += msg.delayTicks;
                    schedule(cumulativeDelay, spaceTicks, msg.string, 
                            historyEnabled, hudMessageEnabled);
                }
            }
        }
    }

    // Scheduling

    private transient final List<ScheduledMessage> scheduledMessages = new ArrayList<>();
    
    public void clearScheduled() {
        scheduledMessages.clear();
    }

    public boolean hasRepeating() {
        for (ScheduledMessage msg : scheduledMessages) {
            if (msg.repeatDelay != -1) return true;
        }
        return false;
    }

    public void stopRepeating() {
        scheduledMessages.removeIf((msg) -> msg.repeatDelay != -1);
    }

    private void schedule(int initialDelay, int repeatDelay, String message,
                          boolean addToHistory, boolean showHudMsg) {
        scheduledMessages.add(new ScheduledMessage(initialDelay, repeatDelay, message,
                addToHistory, showHudMsg));
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
                if (repeatDelay != -1) delay = repeatDelay;
                else return true;
            }
            return false;
        }
    }

    // Deserialization

    public static class Deserializer implements JsonDeserializer<Macro> {
        @Override
        public Macro deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            int version = obj.has("version") ? obj.get("version").getAsInt() : 0;

            boolean addToHistory = version >= 3 ? obj.get("addToHistory").getAsBoolean() : false;
            boolean showHudMessage = version >= 3 ? obj.get("showHudMessage").getAsBoolean() : false;
            boolean ignoreRatelimit = version >= 4 ? obj.get("ignoreRatelimit").getAsBoolean() : false;

            ConflictStrategy conflictStrategy = version >= 3
                    ? ConflictStrategy.valueOf(obj.get("conflictStrategy").getAsString())
                    : getConflictStrategy(obj.get("conflictStrategy").getAsString());
            SendMode sendMode = version >= 3
                    ? SendMode.valueOf(obj.get("sendMode").getAsString())
                    : getSendMode(obj.get("sendStrategy").getAsString());

            int spaceTicks = version >= 1 ? obj.get("spaceTicks").getAsInt() : 0;
            
            Keybind keybind = version >= 4
                    ? ctx.deserialize(obj.get("keybind"), Keybind.class)
                    : version == 3 
                        ? new Keybind(
                            InputConstants.getKey(obj.get("keyName").getAsString()), 
                            InputConstants.getKey(obj.get("limitKeyName").getAsString()))
                        : new Keybind(
                            InputConstants.getKey(obj.getAsJsonObject("key").get("name").getAsString()),
                            InputConstants.getKey(obj.getAsJsonObject("limitKey").get("name").getAsString()));
            Keybind altKeybind = version >= 4 
                    ? ctx.deserialize(obj.get("altKeybind"), Keybind.class) 
                    : new Keybind();
            
            List<Message> messages = new ArrayList<>();
            for (JsonElement je : obj.getAsJsonArray("messages")) {
                Message message = version >= 2
                        ? ctx.deserialize(je, Message.class)
                        : new Message(true, je.getAsString(), 0);
                if (message != null) messages.add(message);
            }

            // Validate
            if (spaceTicks < 0) throw new JsonParseException("Macro Error: spaceTicks < 0");

            return new Macro(addToHistory, showHudMessage, ignoreRatelimit, conflictStrategy,
                    sendMode, spaceTicks, keybind, altKeybind, messages);
        }

        public static ConflictStrategy getConflictStrategy(String str) {
            return switch(str) {
                case "ZERO" -> ConflictStrategy.SUBMIT;
                case "ONE" -> ConflictStrategy.ASSERT;
                case "TWO" -> ConflictStrategy.VETO;
                case "THREE" -> ConflictStrategy.AVOID;
                default -> throw new JsonParseException("Macro Error: ConflictStrategy " + str);
            };
        }

        public static SendMode getSendMode(String str) {
            return switch(str) {
                case "ZERO" -> SendMode.SEND;
                case "ONE" -> SendMode.TYPE;
                case "TWO" -> SendMode.CYCLE;
                default -> throw new JsonParseException("Macro Error: SendMode " + str);
            };
        }
    }
}
