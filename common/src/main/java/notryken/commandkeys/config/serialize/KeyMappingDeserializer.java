package notryken.commandkeys.config.serialize;

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

        /*
        "name"
        Fabric/Quilt:   field_1660
        Forge:          f_90813_
        NeoForge:       name

        "key"
        Fabric/Quilt:   field_1655
        Forge:          f_90816_
        NeoForge:       key

        "category"
        Fabric/Quilt:   field_1659
        Forge:          f_90815_
        NeoForge:       category
         */

        try {
            return new KeyMapping(jsonObject.get("name").getAsString(),
                    gson.fromJson(jsonObject.get("key"), InputConstants.Key.class).getValue(),
                    jsonObject.get("category").getAsString());
        }
        catch (NullPointerException e1) {
            try {
                return new KeyMapping(jsonObject.get("field_1660").getAsString(),
                        gson.fromJson(jsonObject.get("field_1655"), InputConstants.Key.class).getValue(),
                        jsonObject.get("field_1659").getAsString());
            }
            catch (NullPointerException e2) {
                try {
                    return new KeyMapping(jsonObject.get("f_90813_").getAsString(),
                            gson.fromJson(jsonObject.get("f_90816_"), InputConstants.Key.class).getValue(),
                            jsonObject.get("f_90815_").getAsString());
                }
                catch (NullPointerException e3) {
                    throw new JsonParseException("Could not parse KeyMapping: No key found.", e1);
                }
            }
        }
    }
}
