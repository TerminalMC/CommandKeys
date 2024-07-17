/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.config;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Consists of behavioral options, a key and modifier key (both of which can be
 * unbound), and a list of {@link Message} instances.
 *
 * <p>Tightly coupled to a {@link Profile}, to allow updating the
 * {@link Profile#commandKeyMap}.</p>
 */
public class CommandKey {
    public final int version = 2;

    public transient final Profile profile;

    /**
     * ZERO: Submit, ONE: Assert, TWO: Veto, THREE: Avoid
     */
    public final QuadState conflictStrategy;
    /**
     * ZERO: Send, ONE: Type, TWO: Cycle
     */
    public final TriState sendStrategy;
    public int spaceTicks; // Delay between messages if sending
    public transient int cycleIndex; // Index of next message if cycling

    private InputConstants.Key key;
    private InputConstants.Key limitKey;

    final List<Message> messages;

    /**
     * Creates a default empty instance.
     */
    public CommandKey(Profile profile) {
        this.profile = profile;
        this.conflictStrategy = new QuadState(Config.get().defaultConflictStrategy.state);
        this.sendStrategy = new TriState(Config.get().defaultSendMode.state);
        this.spaceTicks = 0;
        this.cycleIndex = 0;
        this.key = InputConstants.UNKNOWN;
        this.limitKey = InputConstants.UNKNOWN;
        this.messages = new ArrayList<>();
    }

    /**
     * Not validated, only for use by self-validating deserializer.
     */
    private CommandKey(Profile profile, QuadState conflictStrategy,
                      TriState sendStrategy, int spaceTicks, InputConstants.Key key,
                      InputConstants.Key limitKey, List<Message> messages) {
        this.profile = profile;
        this.conflictStrategy = conflictStrategy;
        this.sendStrategy = sendStrategy;
        this.spaceTicks = spaceTicks;
        this.cycleIndex = 0;
        this.key = key;
        this.limitKey = limitKey;
        this.messages = messages;
        profile.commandKeyMap.put(key, this);
    }

    public InputConstants.Key getKey() {
        return key;
    }

    /**
     * Sets the bound key to the specified value, and updates the coupled
     * {@link Profile#commandKeyMap}.
     */
    public void setKey(InputConstants.Key key) {
        this.key = key;
        profile.rebuildCmdKeyMap();
    }

    public InputConstants.Key getLimitKey() {
        return limitKey;
    }

    public void setLimitKey(InputConstants.Key limitKey) {
        this.limitKey = limitKey;
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

    // Serialization / Deserialization

    /**
     * Required due to use of {@link InputConstants.Key} instances.
     */
    public static class Serializer implements JsonSerializer<CommandKey> {
        @Override
        public JsonElement serialize(CommandKey src, Type typeOfSrc, JsonSerializationContext ctx) {
            JsonObject obj = new JsonObject();
            obj.addProperty("version", src.version);

            obj.addProperty("conflictStrategy", src.conflictStrategy.state.toString());
            obj.addProperty("sendStrategy", src.sendStrategy.state.toString());
            obj.addProperty("spaceTicks", src.spaceTicks);

            JsonObject keyObj = new JsonObject();
            keyObj.addProperty("name", src.key.getName());
            keyObj.addProperty("type", src.key.getType().toString());
            keyObj.addProperty("value", src.key.getValue());
            obj.add("key", keyObj);

            JsonObject limitKeyObj = new JsonObject();
            limitKeyObj.addProperty("name", src.limitKey.getName());
            limitKeyObj.addProperty("type", src.limitKey.getType().toString());
            limitKeyObj.addProperty("value", src.limitKey.getValue());
            obj.add("limitKey", limitKeyObj);

            JsonArray messagesObj = new JsonArray();
            for (Message message : src.messages) messagesObj.add(ctx.serialize(message));
            obj.add("messages", messagesObj);

            return obj;
        }
    }

    public static class Deserializer implements JsonDeserializer<CommandKey> {
        Profile profile;

        public Deserializer(Profile profile) {
            this.profile = profile;
        }

        @Override
        public CommandKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            int version = obj.has("version") ? obj.get("version").getAsInt() : 0;

            QuadState conflictStrategy = new QuadState(obj.get("conflictStrategy").getAsString());
            TriState sendStrategy = new TriState(obj.get("sendStrategy").getAsString());
            int spaceTicks;
            InputConstants.Key key = InputConstants.getKey(obj.getAsJsonObject("key").get("name").getAsString());
            InputConstants.Key limitKey = InputConstants.getKey(obj.getAsJsonObject("limitKey").get("name").getAsString());
            List<Message> messages = new ArrayList<>();
            for (JsonElement je : obj.getAsJsonArray("messages")) {
                messages.add(version >= 2
                        ? ctx.deserialize(je, Message.class)
                        : new Message(true, je.getAsString(), 0));
            }

            if (version == 0) {
                spaceTicks = 0;
            } else {
                spaceTicks = obj.get("spaceTicks").getAsInt();
            }

            // Validate
            if (spaceTicks < -1) throw new JsonParseException("CommandKey #1");

            return new CommandKey(profile, conflictStrategy, sendStrategy, spaceTicks, key, limitKey, messages);
        }
    }
}
