/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.config;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.CommandKeys;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Consists of behavioral options, a key and modifier key (both of which can be
 * unbound), and a list of {@link Message} instances.
 *
 * <p>Tightly coupled to a {@link Profile}, to allow updating the
 * {@link Profile#keyMacroMap}.</p>
 */
public class Macro {
    public final int version = 3;

    public enum ConflictStrategy {
        SUBMIT,
        ASSERT,
        VETO,
        AVOID
    }

    public enum SendMode {
        SEND,
        TYPE,
        CYCLE,
        RANDOM,
        REPEAT,
    }

    public static final Random RANDOM = new Random();

    public transient final Profile profile;

    public boolean addToHistory;
    public boolean showHudMessage;
    private ConflictStrategy conflictStrategy;
    private SendMode sendMode;
    public int spaceTicks; // Delay between messages if sending
    public transient int cycleIndex; // Index of next message if cycling

    private transient InputConstants.Key key;
    private String keyName;
    private transient InputConstants.Key limitKey;
    private String limitKeyName;

    final List<Message> messages;

    /**
     * Creates a default empty instance.
     */
    public Macro(Profile profile) {
        this.addToHistory = false;
        this.showHudMessage = false;
        this.profile = profile;
        this.conflictStrategy = Config.get().defaultConflictStrategy;
        this.sendMode = Config.get().defaultSendMode;
        this.spaceTicks = 0;
        this.cycleIndex = 0;
        this.key = InputConstants.UNKNOWN;
        this.keyName = key.getName();
        this.limitKey = InputConstants.UNKNOWN;
        this.limitKeyName = limitKey.getName();
        this.messages = new ArrayList<>();
    }

    /**
     * Not validated, only for use by self-validating deserializer.
     */
    private Macro(Profile profile, boolean addToHistory, boolean showHudMessage,
                  ConflictStrategy conflictStrategy, SendMode sendMode, int spaceTicks,
                  InputConstants.Key key, InputConstants.Key limitKey, List<Message> messages) {
        this.profile = profile;
        this.addToHistory = addToHistory;
        this.showHudMessage = showHudMessage;
        this.conflictStrategy = conflictStrategy;
        this.sendMode = sendMode;
        this.spaceTicks = spaceTicks;
        this.cycleIndex = 0;
        this.key = key;
        this.keyName = key.getName();
        this.limitKey = limitKey;
        this.limitKeyName = limitKey.getName();
        this.messages = messages;
        profile.keyMacroMap.put(key, this);
    }

    public boolean addToHistory() {
        return switch(profile.addToHistory) {
            case ON -> true;
            case OFF -> false;
            case DEFER -> addToHistory;
        };
    }

    public boolean showHudMessage() {
        return switch(profile.showHudMessage) {
            case ON -> true;
            case OFF -> false;
            case DEFER -> showHudMessage;
        };
    }

    public ConflictStrategy getConflictStrategy() {
        return conflictStrategy;
    }

    public void setConflictStrategy(ConflictStrategy conflictStrategy) {
        stopRepeating();
        this.conflictStrategy = conflictStrategy;
    }

    public SendMode getSendMode() {
        return sendMode;
    }

    public void setSendMode(SendMode sendMode) {
        stopRepeating();
        this.sendMode = sendMode;
    }

    public InputConstants.Key getKey() {
        return key;
    }

    /**
     * Sets the bound key to the specified value, and updates the coupled
     * {@link Profile#keyMacroMap}.
     */
    public void setKey(InputConstants.Key key) {
        stopRepeating();
        this.key = key;
        this.keyName = key.getName();
        profile.rebuildMacroMap();
    }

    public InputConstants.Key getLimitKey() {
        return limitKey;
    }

    public void setLimitKey(InputConstants.Key limitKey) {
        stopRepeating();
        this.limitKey = limitKey;
        this.limitKeyName = limitKey.getName();
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

    public void trigger() {
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
                    schedule(cumulativeDelay, -1, msg.string, addToHistory(), showHudMessage());
                }
            }
            case TYPE -> {
                if (!messages.isEmpty()) {
                    CommandKeys.type(messages.getFirst().string);
                }
            }
            case CYCLE -> {
                // Allow spacer blank messages, and multiple messages per press.
                for (String msg : messages.get(cycleIndex).string.split(",,")) {
                    if (!msg.isBlank()) {
                        CommandKeys.send(msg, addToHistory(), showHudMessage());
                    }
                }
                if (++cycleIndex >= messages.size()) cycleIndex = 0;
            }
            case RANDOM -> {
                if (!messages.isEmpty()) {
                    Message msg = messages.get(RANDOM.nextInt(messages.size()));
                    if (!msg.string.isBlank()) {
                        CommandKeys.send(msg.string, addToHistory(), showHudMessage());
                    }
                }
            }
            case REPEAT -> {
                int cumulativeDelay = 0;
                for (Message msg : messages) {
                    cumulativeDelay += msg.delayTicks;
                    schedule(cumulativeDelay, spaceTicks, msg.string,
                            addToHistory(), showHudMessage());
                }
            }
        }
    }

    // Scheduling

    private transient final List<ScheduledMessage> scheduledMessages = new ArrayList<>();

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
        Profile profile;

        public Deserializer(Profile profile) {
            this.profile = profile;
        }

        @Override
        public Macro deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            int version = obj.has("version") ? obj.get("version").getAsInt() : 0;

            boolean addToHistory = version >= 3 ? obj.get("addToHistory").getAsBoolean() : false;
            boolean showHudMessage = version >= 3 ? obj.get("showHudMessage").getAsBoolean() : false;

            ConflictStrategy conflictStrategy = version >= 3
                    ? ConflictStrategy.valueOf(obj.get("conflictStrategy").getAsString())
                    : getConflictStrategy(obj.get("conflictStrategy").getAsString());
            SendMode sendMode = version >= 3
                    ? SendMode.valueOf(obj.get("sendMode").getAsString())
                    : getSendMode(obj.get("sendStrategy").getAsString());

            int spaceTicks = version >= 1 ? obj.get("spaceTicks").getAsInt() : 0;

            InputConstants.Key key = version >= 3
                    ? InputConstants.getKey(obj.get("keyName").getAsString())
                    : InputConstants.getKey(obj.getAsJsonObject("key").get("name").getAsString());
            InputConstants.Key limitKey = version >= 3
                    ? InputConstants.getKey(obj.get("limitKeyName").getAsString())
                    : InputConstants.getKey(obj.getAsJsonObject("limitKey").get("name").getAsString());
            List<Message> messages = new ArrayList<>();
            for (JsonElement je : obj.getAsJsonArray("messages")) {
                messages.add(version >= 2
                        ? ctx.deserialize(je, Message.class)
                        : new Message(true, je.getAsString(), 0));
            }

            // Validate
            if (spaceTicks < 0) throw new JsonParseException("Macro #1");

            return new Macro(profile, addToHistory, showHudMessage, conflictStrategy,
                    sendMode, spaceTicks, key, limitKey, messages);
        }

        public static ConflictStrategy getConflictStrategy(String str) {
            return switch(str) {
                case "ZERO" -> ConflictStrategy.SUBMIT;
                case "ONE" -> ConflictStrategy.ASSERT;
                case "TWO" -> ConflictStrategy.VETO;
                case "THREE" -> ConflictStrategy.AVOID;
                default -> throw new JsonParseException("Macro #2");
            };
        }

        public static SendMode getSendMode(String str) {
            return switch(str) {
                case "ZERO" -> SendMode.SEND;
                case "ONE" -> SendMode.TYPE;
                case "TWO" -> SendMode.CYCLE;
                default -> throw new JsonParseException("Macro #3");
            };
        }
    }
}
