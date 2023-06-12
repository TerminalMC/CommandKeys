package notryken.quickmessages.config;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class ConfigDeserializer implements JsonDeserializer<Config>
{
    @Override
    public Config deserialize(JsonElement jsonElement, Type type,
                              JsonDeserializationContext context)
            throws JsonParseException
    {
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        ArrayList<String[]> keyMessages = new ArrayList<>();

        try {
            JsonArray jsonArray1 =
                    jsonObject.get("keyMessages").getAsJsonArray();

            for (JsonElement je : jsonArray1) {
                JsonArray jsonArray2 = je.getAsJsonArray();
                if (jsonArray2.size() == 2) {
                    keyMessages.add(new String[]{
                            jsonArray2.get(0).getAsString(),
                            jsonArray2.get(1).getAsString()
                    });
                }
            }

            return new Config(keyMessages);
        }
        catch (JsonParseException | NullPointerException |
               UnsupportedOperationException | IllegalStateException e)
        {
            if (keyMessages.size() > 0) {
                return new Config(keyMessages);
            }
            return null;
        }
    }
}
