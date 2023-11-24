package notryken.quickmessages.config.deserialize;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import java.lang.reflect.Type;

public class KeyMappingDeserializer implements JsonDeserializer<KeyMapping> {
    @Override
    public KeyMapping deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject jsonObject = json.getAsJsonObject();

        Gson gson = new GsonBuilder().registerTypeAdapter(InputConstants.Key.class,
                new InputConstantsKeyDeserializer()).create();

        return new KeyMapping(jsonObject.get("name").getAsString(),
                gson.fromJson(jsonObject.get("key"), InputConstants.Key.class).getValue(),
                        jsonObject.get("category").getAsString());
    }
}
