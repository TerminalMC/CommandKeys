package notryken.commandkeys.config.deserialize;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;

import java.lang.reflect.Type;

public class InputConstantsKeyDeserializer implements JsonDeserializer<InputConstants.Key> {
    @Override
    public InputConstants.Key deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject jsonObject = json.getAsJsonObject();

        /*
        "name"
        Fabric/Quilt:   field_1663
        Forge:          f_84853_
        NeoForge:       name
         */

        try {
            return InputConstants.getKey(jsonObject.get("name").getAsString());
        }
        catch (NullPointerException e1) {
            try {
                return InputConstants.getKey(jsonObject.get("field_1663").getAsString());
            }
            catch (NullPointerException e2) {
                try {
                    return InputConstants.getKey(jsonObject.get("f_84853_").getAsString());
                }
                catch (NullPointerException e3) {
                    throw new JsonParseException("Could not parse InputConstants.Key: No key found.", e1);
                }
            }
        }
    }
}
