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
import dev.terminalmc.commandkeys.util.JsonUtil;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public class Message {
    public static final int VERSION = 1;
    public final int version = VERSION;

    public String string;
    public int delayTicks;

    /**
     * Creates a blank default instance.
     */
    public Message() {
        this("", 0);
    }

    /**
     * Not validated, only for use by default constructor and self-validating
     * deserializer.
     */
    Message(String string, int delayTicks) {
        this.string = string;
        this.delayTicks = delayTicks;
    }

    /**
     * Copy constructor.
     */
    Message(Message message) {
        this.string = message.string;
        this.delayTicks = message.delayTicks;
    }

    // Validation

    Message validate() {
        if (string == null) string = "";
        if (delayTicks < 0) delayTicks = 0;
        return this;
    }

    // Deserialization

    public static class Deserializer implements JsonDeserializer<Message> {
        @Override
        public @Nullable Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            int version = obj.get("version").getAsInt();
            boolean silent = version != VERSION;

            String string = JsonUtil.getOrDefault(obj, "string",
                    "", silent);

            int delayTicks = JsonUtil.getOrDefault(obj, "delayTicks",
                    0, silent);

            return new Message(
                    string,
                    delayTicks
            ).validate();
        }
    }
}
