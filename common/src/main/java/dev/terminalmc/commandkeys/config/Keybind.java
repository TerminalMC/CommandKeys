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
import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.util.JsonUtil;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Consists of two {@link InputConstants.Key} instances, allowing both single
 * and dual-key activation.
 */
public class Keybind {
    public static final int VERSION = 0;
    public final int version = VERSION;

    private transient InputConstants.Key key;
    private String keyName;
    private transient InputConstants.Key limitKey;
    private String limitKeyName;

    /**
     * Creates a default instance.
     */
    public Keybind() {
        this.key = InputConstants.UNKNOWN;
        this.keyName = key.getName();
        this.limitKey = InputConstants.UNKNOWN;
        this.limitKeyName = limitKey.getName();
    }

    /**
     * Not validated. Only for use by self-validating deserializer.
     */
    Keybind(InputConstants.Key key, InputConstants.Key limitKey) {
        this.key = key;
        this.keyName = key.getName();
        this.limitKey = limitKey;
        this.limitKeyName = limitKey.getName();
    }

    /**
     * Copy constructor.
     */
    Keybind(Keybind keybind) {
        this.key = keybind.key;
        this.keyName = keybind.keyName;
        this.limitKey = keybind.limitKey;
        this.limitKeyName = keybind.limitKeyName;
    }

    public InputConstants.Key getKey() {
        return key;
    }
    
    void setKey(InputConstants.Key key) {
        this.key = key;
        this.keyName = key.getName();
    }

    public InputConstants.Key getLimitKey() {
        return limitKey;
    }

    void setLimitKey(InputConstants.Key limitKey) {
        this.limitKey = limitKey;
        this.limitKeyName = limitKey.getName();
    }

    public boolean isKeyDown() {
        return !key.equals(InputConstants.UNKNOWN) && InputConstants.isKeyDown(
                Minecraft.getInstance().getWindow().getWindow(), key.getValue());
    }

    public boolean isLimitKeyDown() {
        return !limitKey.equals(InputConstants.UNKNOWN) && InputConstants.isKeyDown(
                Minecraft.getInstance().getWindow().getWindow(), limitKey.getValue());
    }
    
    boolean isDown() {
        return isKeyDown() && (limitKey.equals(InputConstants.UNKNOWN) || isLimitKeyDown());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Keybind keybind)) return false;
        return key.equals(keybind.key) && limitKey.equals(keybind.limitKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, limitKey);
    }
    
    // Validation
    
    Keybind validate() {
        // If main key is unbound, limit key cannot be bound
        if (key.equals(InputConstants.UNKNOWN) && !limitKey.equals(InputConstants.UNKNOWN)) {
            limitKey = InputConstants.UNKNOWN;
        }
        // Update names just because
        keyName = key.getName();
        limitKeyName = limitKey.getName();
        return this;
    }

    // Deserialization

    public static class Deserializer implements JsonDeserializer<Keybind> {
        @Override
        public Keybind deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) 
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            int version = obj.get("version").getAsInt();
            boolean silent = version != VERSION;
            
            InputConstants.Key key = JsonUtil.getOrDefault(obj, "keyName",
                    InputConstants.UNKNOWN, silent);
            InputConstants.Key limitKey = JsonUtil.getOrDefault(obj, "limitKeyName",
                    InputConstants.UNKNOWN, silent);

            return new Keybind(
                    key,
                    limitKey
            ).validate();
        }
    }
}
