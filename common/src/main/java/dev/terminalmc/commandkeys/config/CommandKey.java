/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.config;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Contains behavioral options, a key and modifier key (both of which can be
 * unbound), and a list of messages.</p>
 *
 * <p>Strongly coupled to a {@link Profile}, to allow management of the
 * profile's Key-CommandKey map.</p>
 */
public class CommandKey {
    public final int version = 1;

    public transient final Profile profile;

    public final QuadState conflictStrategy; // Submit, Assert, Veto or Avoid
    public final TriState sendStrategy; // Send, Type or Cycle
    public int spaceTicks;
    public transient int cycleIndex; // Index of next message if cycling

    private InputConstants.Key key;
    private InputConstants.Key limitKey;

    public final List<String> messages;

    /**
     * Creates a default empty instance.
     */
    public CommandKey(Profile profile) {
        this.profile = profile;
        this.conflictStrategy = new QuadState();
        this.sendStrategy = new TriState();
        this.spaceTicks = 0;
        this.cycleIndex = 0;
        this.key = InputConstants.UNKNOWN;
        this.limitKey = InputConstants.UNKNOWN;
        this.messages = new ArrayList<>();
    }

    /**
     * <p>Not validated, only for use by self-validating deserializer.</p>
     */
    private CommandKey(Profile profile, QuadState conflictStrategy,
                      TriState sendStrategy, int spaceTicks, InputConstants.Key key,
                      InputConstants.Key limitKey, List<String> messages) {
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
     * <p>Sets the bound key to the specified value, and updates the coupled
     * profile's Key-CommandKey map.</p>
     */
    public void setKey(InputConstants.Key key) {
        profile.commandKeyMap.remove(this.key, this);
        this.key = key;
        profile.commandKeyMap.put(this.key, this);
    }

    public InputConstants.Key getLimitKey() {
        return limitKey;
    }

    public void setLimitKey(InputConstants.Key limitKey) {
        this.limitKey = limitKey;
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
            for (String message : src.messages) messagesObj.add(message);
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
            ArrayList<String> messages = new ArrayList<>();
            for (JsonElement je : obj.getAsJsonArray("messages")) messages.add(je.getAsString());

            if (version == 0) {
                spaceTicks = 0;
            } else {
                spaceTicks = obj.get("spaceTicks").getAsInt();
            }

            // Validate
            if (spaceTicks < 0) throw new JsonParseException("CommandKey #1");

            return new CommandKey(profile, conflictStrategy, sendStrategy, spaceTicks, key, limitKey, messages);
        }
    }
}
