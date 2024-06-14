/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.config;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.config.util.JsonRequired;
import dev.terminalmc.commandkeys.config.util.JsonValidator;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/*
FIXME: This comment is out of date

Vanilla Minecraft keybind handling works roughly like this:

Minecraft.options.keyMappings is a hardcoded final KeyMapping[], initialised on
game load.
Only KeyMappings in this Array will be placed on the Minecraft Controls screen.
Adding a KeyMapping to this Array requires using a loader-specific keybind-
registration operation, and cannot be done while the game is running.

KeyMapping.ALL is a static final Map<String, KeyMapping>, which maps km.name to
km, on creation of any KeyMapping 'km'.

KeyMapping.MAP is a static final Map<InputConstants.Key, KeyMapping>, which maps
km.key to km, on creation of any KeyMapping 'km'.

KeyMapping.MAP is reset using KeyMapping::resetMapping, called by
KeyBindsList::resetMappingAndUpdateButtons, which is itself called whenever;
- a KeyBindsList keybind reset button is pressed,
- a KeyBindsList keybind button is selected,
- KeyBindsScreen::mouseClicked is called and KeyBindsScreen.selectedKey != null,
- KeyBindsScreen::keyPressed is called and KeyBindsScreen.selectedKey != null,
- the KeyBindsScreen full reset button is pressed.

KeyMapping::resetMapping simply iterates through KeyMapping.ALL and, for each
KeyMapping 'km', maps km.key to km.

Consequently, if KeyMapping.ALL contains two KeyMapping objects 'km1', 'km2'
such that km1.key.equals(km2.key), and km2 is sequentially later in the
iteration than km1, the value of KeyMapping.MAP.get(km1.key) will be km2.
This, combined with the fact that KeyMapping::click and KeyMapping::set both use
KeyMapping.MAP::get, is why it is not possible to bind a single
InputConstants.Key to multiple KeyMapping objects.

For detecting a key-press, the options are (essentially);

1. On-tick check of KeyMapping::consumeClick (or isDown but whatever)
    - Pros: Simple
    - Cons: Impossible to overlap with bound keys

2. @Inject mixins to the KeyMapping::click call of MouseHandler::onPress and the
KeyMapping::click call of KeyboardHandler::keyPress
    - Pros: Allows overlap with bound keys
    - Cons:

3. On-tick check of InputConstants::isKeyDown
    - Pros: Allows overlap with bound keys
    - Cons: Need to track Key state from previous tick to determine when a press
    is initiated, else would send the message every tick that Key is held.
 */


public class CommandKey {
    public final int version = 1;

    public transient final Profile profile;

    @JsonRequired
    public final QuadState conflictStrategy; // Submit, Assert, Veto or Avoid
    @JsonRequired
    public final TriState sendStrategy; // Send, Type or Cycle
    public transient int cycleIndex; // Index of next message if cycling

    @JsonRequired
    private InputConstants.Key key;
    @JsonRequired
    private InputConstants.Key limitKey;

    @JsonRequired
    public final List<String> messages;

    public CommandKey(Profile profile) {
        this.profile = profile;
        this.conflictStrategy = new QuadState();
        this.sendStrategy = new TriState();
        this.cycleIndex = 0;
        this.key = InputConstants.UNKNOWN;
        this.limitKey = InputConstants.UNKNOWN;
        this.messages = new ArrayList<>();
    }

    public CommandKey(Profile profile, QuadState conflictStrategy, TriState sendStrategy,
                      InputConstants.Key key, InputConstants.Key limitKey,
                      List<String> messages) {
        this.profile = profile;
        this.conflictStrategy = conflictStrategy;
        this.sendStrategy = sendStrategy;
        this.cycleIndex = 0;
        this.key = key;
        this.limitKey = limitKey;
        this.messages = messages;
        profile.COMMANDKEY_MAP.put(key, this);
    }

    public InputConstants.Key getKey() {
        return key;
    }

    public void setKey(InputConstants.Key key) {
        profile.COMMANDKEY_MAP.remove(this.key, this);
        this.key = key;
        profile.COMMANDKEY_MAP.put(this.key, this);
    }

    public InputConstants.Key getLimitKey() {
        return limitKey;
    }

    public void setLimitKey(InputConstants.Key limitKey) {
        this.limitKey = limitKey;
    }

    // Serialization / Deserialization

    public static class Serializer implements JsonSerializer<CommandKey> {
        @Override
        public JsonElement serialize(CommandKey src, Type typeOfSrc, JsonSerializationContext ctx) {
            JsonObject obj = new JsonObject();

            obj.addProperty("conflictStrategy", src.conflictStrategy.state.toString());
            obj.addProperty("sendStrategy", src.sendStrategy.state.toString());

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

        public Deserializer(@NotNull Profile profile) {
            this.profile = profile;
        }

        @Override
        public CommandKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();

            QuadState conflictStrategy = new QuadState(obj.get("conflictStrategy").getAsString());
            TriState sendStrategy = new TriState(obj.get("sendStrategy").getAsString());
            InputConstants.Key key = InputConstants.getKey(obj.getAsJsonObject("key").get("name").getAsString());
            InputConstants.Key limitKey = InputConstants.getKey(obj.getAsJsonObject("limitKey").get("name").getAsString());
            ArrayList<String> messages = new ArrayList<>();
            for (JsonElement je : obj.getAsJsonArray("messages")) messages.add(je.getAsString());

            return new JsonValidator<CommandKey>().validateRequired(
                    new CommandKey(profile, conflictStrategy, sendStrategy, key, limitKey, messages));
        }
    }
}
