/*
 * Copyright 2025 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.terminalmc.commandkeys.config;

import com.google.gson.*;
import dev.terminalmc.commandkeys.CommandKeys;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public class Message {
    public final int version = 1;

    private boolean enabled; // v1 parity
    public String string;
    public int delayTicks;

    /**
     * Creates a default instance.
     */
    public Message() {
        enabled = true;
        string = "";
        delayTicks = 0;
    }

    /**
     * Not validated, only for use by self-validating deserializer.
     */
    Message(boolean enabled, String string, int delayTicks) {
        this.enabled = enabled;
        this.string = string;
        this.delayTicks = delayTicks;
    }

    public static class Deserializer implements JsonDeserializer<Message> {
        @Override
        public @Nullable Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            try {
                int version = obj.get("version").getAsInt();
                
                boolean enabled = obj.get("enabled").getAsBoolean();
                String string = obj.get("string").getAsString();
                int delayTicks = obj.get("delayTicks").getAsInt();

                // Validation
                if (delayTicks < 0) throw new JsonParseException("ResponseMessage Error: delayTicks < 0");

                return new Message(enabled, string, delayTicks);
            }
            catch (Exception e) {
                CommandKeys.LOG.warn("Unable to deserialize ResponseMessage", e);
                return null;
            }
        }
    }
}
