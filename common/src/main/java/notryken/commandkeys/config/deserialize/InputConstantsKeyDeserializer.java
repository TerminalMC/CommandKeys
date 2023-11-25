package notryken.commandkeys.config.deserialize;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;

import java.lang.reflect.Type;

public class InputConstantsKeyDeserializer implements JsonDeserializer<InputConstants.Key> {
    @Override
    public InputConstants.Key deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject jsonObject = json.getAsJsonObject();

        return InputConstants.getKey(jsonObject.get("name").getAsString());
    }
}
