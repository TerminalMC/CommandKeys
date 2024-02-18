package com.notryken.commandkeys.config;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;

/*
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

    public transient final Profile profile;

    public final TriState conflictStrategy; // Submissive, Assertive or Aggressive
    public final TriState sendStrategy; // Send, Type or Cycle
    public transient int cycleIndex; // Index of next message if cycling

    private InputConstants.Key key;
    private InputConstants.Key limitKey;

    public final ArrayList<String> messages;

    public CommandKey(Profile profile) {
        this.profile = profile;
        this.conflictStrategy = new TriState();
        this.sendStrategy = new TriState();
        this.cycleIndex = 0;
        this.key = InputConstants.UNKNOWN;
        this.limitKey = InputConstants.UNKNOWN;
        this.messages = new ArrayList<>();
    }

    public CommandKey(Profile profile, TriState conflictStrategy, TriState sendStrategy,
                      InputConstants.Key key, InputConstants.Key limitKey,
                      ArrayList<String> messages) {
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
        public JsonElement serialize(CommandKey src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject cmdKeyObj = new JsonObject();

            cmdKeyObj.addProperty("conflictStrategy", src.conflictStrategy.state.toString());
            cmdKeyObj.addProperty("sendStrategy", src.sendStrategy.state.toString());

            JsonObject keyObj = new JsonObject();
            keyObj.addProperty("name", src.key.getName());
            keyObj.addProperty("type", src.key.getType().toString());
            keyObj.addProperty("value", src.key.getValue());
            cmdKeyObj.add("key", keyObj);

            JsonObject limitKeyObj = new JsonObject();
            limitKeyObj.addProperty("name", src.limitKey.getName());
            limitKeyObj.addProperty("type", src.limitKey.getType().toString());
            limitKeyObj.addProperty("value", src.limitKey.getValue());
            cmdKeyObj.add("limitKey", limitKeyObj);

            JsonArray messagesObj = new JsonArray();
            for (String message : src.messages) messagesObj.add(message);
            cmdKeyObj.add("messages", messagesObj);

            return cmdKeyObj;
        }
    }

    public static class Deserializer implements JsonDeserializer<CommandKey> {
        Profile profile;

        public Deserializer(@NotNull Profile profile) {
            this.profile = profile;
        }

        @Override
        public CommandKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject cmdKeyObj = json.getAsJsonObject();

            TriState conflictStrategy;
            TriState sendStrategy;
            InputConstants.Key key;
            InputConstants.Key limitKey;
            ArrayList<String> messages = new ArrayList<>();

            conflictStrategy = new TriState(TriState.State.valueOf(cmdKeyObj.get("conflictStrategy").getAsString()));
            sendStrategy = new TriState(TriState.State.valueOf(cmdKeyObj.get("sendStrategy").getAsString()));

            JsonObject keyObj = cmdKeyObj.getAsJsonObject("key");
            key = InputConstants.getKey(keyObj.get("name").getAsString());

            JsonObject limitKeyObj = cmdKeyObj.getAsJsonObject("limitKey");
            limitKey = InputConstants.getKey(limitKeyObj.get("name").getAsString());

            JsonArray messagesObj = cmdKeyObj.getAsJsonArray("messages");
            for (JsonElement element : messagesObj) messages.add(element.getAsString());

            return new CommandKey(profile, conflictStrategy, sendStrategy, key, limitKey, messages);
        }
    }
}
