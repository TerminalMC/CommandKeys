package notryken.commandkeys.config;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;

import java.lang.reflect.Type;

public class KeyPair {
    private InputConstants.Key key;
    private InputConstants.Key limitKey;

    public KeyPair() {
        this.key = InputConstants.UNKNOWN;
        this.limitKey = InputConstants.UNKNOWN;
    }

    public KeyPair(InputConstants.Key key, InputConstants.Key limitKey) {
        this.key = key;
        this.limitKey = limitKey;
    }

    public InputConstants.Key getKey() {
        return key;
    }

    public void setKey(InputConstants.Key key) {
        this.key = key;
    }

    public InputConstants.Key getLimitKey() {
        return limitKey;
    }

    public void setLimitKey(InputConstants.Key limitKey) {
        this.limitKey = limitKey;
    }

    public static class Serializer implements JsonSerializer<KeyPair> {
        @Override
        public JsonElement serialize(KeyPair src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject keyPairObj = new JsonObject();

            JsonObject keyObj = new JsonObject();
            keyObj.addProperty("name", src.key.getName());
            keyObj.addProperty("type", src.key.getType().toString());
            keyObj.addProperty("value", src.key.getValue());
            keyPairObj.add("key", keyObj);

            JsonObject limitKeyObj = new JsonObject();
            limitKeyObj.addProperty("name", src.limitKey.getName());
            limitKeyObj.addProperty("type", src.limitKey.getType().toString());
            limitKeyObj.addProperty("value", src.limitKey.getValue());
            keyPairObj.add("limitKey", limitKeyObj);

            return keyPairObj;
        }
    }

    public static class Deserializer implements JsonDeserializer<KeyPair> {
        @Override
        public KeyPair deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject keyPairObj = json.getAsJsonObject();

            InputConstants.Key key;
            InputConstants.Key limitKey;

            JsonObject keyObj = keyPairObj.getAsJsonObject("key");
            key = InputConstants.getKey(keyObj.get("name").getAsString());

            JsonObject limitKeyObj = keyPairObj.getAsJsonObject("limitKey");
            limitKey = InputConstants.getKey(limitKeyObj.get("name").getAsString());

            return new KeyPair(key, limitKey);
        }
    }
}
