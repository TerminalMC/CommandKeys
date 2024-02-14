package notryken.commandkeys.config;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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


public class CommandMonoKey {
    // TODO check access for all fields

    public static final Map<InputConstants.Key, CommandMonoKey> MAP = new HashMap<>();

    // Key conflict handling
    public TriState conflictStrategy; // Submissive, Assertive or Aggressive

    // Send behaviour
    public QuadState onlyIfKey; // NONE, CTRL, ALT, or SHIFT
    public boolean fullSend; // Send the message (rather than just typing it)
    public boolean cycle; // Whether to cycle through messages
    public int nextIndex; // Next message is messages.get(index)

    private InputConstants.Key key;

    public ArrayList<String> messages;

    public CommandMonoKey() {
        this.conflictStrategy = new TriState();
        this.onlyIfKey = new QuadState();
        this.fullSend = true;
        this.cycle = false;
        this.nextIndex = 0;
        this.key = InputConstants.UNKNOWN;
        this.messages = new ArrayList<>();
    }

    public CommandMonoKey(TriState conflictStrategy, QuadState onlyIfKey,
                          boolean fullSend, boolean cycle, int nextIndex,
                          InputConstants.Key key, ArrayList<String> messages) {
        this.conflictStrategy = conflictStrategy;
        this.onlyIfKey = onlyIfKey;
        this.fullSend = fullSend;
        this.cycle = cycle;
        this.nextIndex = nextIndex;
        this.key = key;
        this.messages = messages;
        MAP.put(key, this);
    }

    public InputConstants.Key getKey() {
        return key;
    }

    public void setKey(InputConstants.Key key) {
        MAP.remove(this.key);
        MAP.put(key, this);
        this.key = key;
    }


    public static class Serializer implements JsonSerializer<CommandMonoKey> {
        @Override
        public JsonElement serialize(CommandMonoKey src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject cmkObj = new JsonObject();

            cmkObj.addProperty("conflictStrategy", src.conflictStrategy.state.toString());
            cmkObj.addProperty("onlyIfKey", src.onlyIfKey.state.toString());
            cmkObj.addProperty("fullSend", src.fullSend);
            cmkObj.addProperty("cycle", src.cycle);
            cmkObj.addProperty("nextIndex", src.nextIndex);

            JsonObject keyObj = new JsonObject();
            keyObj.addProperty("name", src.key.getName());
            keyObj.addProperty("type", src.key.getType().toString());
            keyObj.addProperty("value", src.key.getValue());
            cmkObj.add("key", keyObj);

            JsonArray messagesObj = new JsonArray();
            for (String message : src.messages) messagesObj.add(message);
            cmkObj.add("messages", messagesObj);

            return cmkObj;
        }
    }

    public static class Deserializer implements JsonDeserializer<CommandMonoKey> {
        @Override
        public CommandMonoKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject cmkObj = json.getAsJsonObject();

            TriState conflictStrategy;
            QuadState onlyIfKey;
            boolean fullSend;
            boolean cycle;
            int nextIndex;
            InputConstants.Key key;
            ArrayList<String> messages = new ArrayList<>();

            conflictStrategy = new TriState(TriState.State.valueOf(cmkObj.get("conflictStrategy").getAsString()));
            onlyIfKey = new QuadState(QuadState.State.valueOf(cmkObj.get("onlyIfKey").getAsString()));
            fullSend = cmkObj.get("fullSend").getAsBoolean();
            cycle = cmkObj.get("cycle").getAsBoolean();
            nextIndex = cmkObj.get("nextIndex").getAsInt();

            JsonObject keyObj = cmkObj.getAsJsonObject("key");
            key = InputConstants.getKey(keyObj.get("name").getAsString());

            JsonArray messagesObj = cmkObj.getAsJsonArray("messages");
            for (JsonElement element : messagesObj) messages.add(element.getAsString());

            return new CommandMonoKey(conflictStrategy, onlyIfKey, fullSend,
                    cycle, nextIndex, key, messages);
        }
    }
}
