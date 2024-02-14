package notryken.commandkeys.config;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CommandDualKey {
    // TODO check access for all fields

    public static final Map<InputConstants.Key, CommandDualKey> MAP = new HashMap<>();

    private InputConstants.Key key;
    public ArrayList<String> messages;

    public CommandDualKey() {
        this.key = InputConstants.UNKNOWN;
        this.messages = new ArrayList<>();
    }

    public CommandDualKey(InputConstants.Key key, ArrayList<String> messages) {
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

    public static class Serializer implements JsonSerializer<CommandDualKey> {
        @Override
        public JsonElement serialize(CommandDualKey src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject cdkObj = new JsonObject();

            JsonObject keyObj = new JsonObject();
            keyObj.addProperty("name", src.key.getName());
            keyObj.addProperty("type", src.key.getType().toString());
            keyObj.addProperty("value", src.key.getValue());
            cdkObj.add("key", keyObj);

            JsonArray messagesObj = new JsonArray();
            for (String message : src.messages) messagesObj.add(message);
            cdkObj.add("messages", messagesObj);

            return cdkObj;
        }
    }

    public static class Deserializer implements JsonDeserializer<CommandDualKey> {
        @Override
        public CommandDualKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject cdkObj = json.getAsJsonObject();

            InputConstants.Key key;
            ArrayList<String> messages = new ArrayList<>();

            JsonObject keyObj = cdkObj.getAsJsonObject("key");
            key = InputConstants.getKey(keyObj.get("name").getAsString());

            JsonArray messagesObj = cdkObj.getAsJsonArray("messages");
            for (JsonElement element : messagesObj) messages.add(element.getAsString());

            return new CommandDualKey(key, messages);
        }
    }
}
