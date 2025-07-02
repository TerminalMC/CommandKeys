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

package dev.terminalmc.commandkeys.util;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.commandkeys.CommandKeys;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class JsonUtil {

    public static final String LOG_STR =
            "Unable to deserialize key '{}' with type '{}': {}. Using default value.";
    public static boolean hasChanged = false;

    public static void reset() {
        hasChanged = false;
    }

    public static @Nullable JsonPrimitive getAsJsonPrimitiveOrNull(
            JsonObject obj,
            String key,
            Class<?> cls,
            boolean silent
    ) {
        if (!obj.has(key)) {
            if (!silent)
                CommandKeys.LOG.error(LOG_STR, key, cls.getName(), "Key not found");
            hasChanged = true;
            return null;
        }

        JsonElement element = obj.get(key);
        if (!element.isJsonPrimitive()) {
            if (!silent)
                CommandKeys.LOG.error(
                        LOG_STR,
                        key,
                        cls.getName(),
                        "Value '" + element + "' is not JsonPrimitive"
                );
            hasChanged = true;
            return null;
        }

        return element.getAsJsonPrimitive();
    }

    public static @Nullable JsonObject getAsJsonObjectOrNull(
            JsonObject obj,
            String key,
            Class<?> cls,
            boolean silent
    ) {
        if (!obj.has(key)) {
            if (!silent)
                CommandKeys.LOG.error(LOG_STR, key, cls.getName(), "Key not found");
            hasChanged = true;
            return null;
        }

        JsonElement element = obj.get(key);
        if (!element.isJsonObject()) {
            if (!silent)
                CommandKeys.LOG.error(
                        LOG_STR,
                        key,
                        cls.getName(),
                        "Value '" + element + "' is not JsonObject"
                );
            hasChanged = true;
            return null;
        }

        return element.getAsJsonObject();
    }

    public static @Nullable JsonArray getAsJsonArrayOrNull(
            JsonObject obj,
            String key,
            Class<?> cls,
            boolean silent
    ) {
        if (!obj.has(key)) {
            if (!silent)
                CommandKeys.LOG.error(LOG_STR, key, cls.getName(), "Key not found");
            hasChanged = true;
            return null;
        }

        JsonElement element = obj.get(key);
        if (!element.isJsonArray()) {
            if (!silent)
                CommandKeys.LOG.error(
                        LOG_STR,
                        key,
                        cls.getName(),
                        "Value '" + element + "' is not JsonArray"
                );
            hasChanged = true;
            return null;
        }

        return element.getAsJsonArray();
    }

    /**
     * String deserialization helper.
     */
    public static String getOrDefault(JsonObject obj, String key, String def, boolean silent) {
        Class<?> cls = String.class;
        JsonElement element = getAsJsonPrimitiveOrNull(obj, key, cls, silent);
        if (element == null)
            return def;

        if (!element.getAsJsonPrimitive().isString()) {
            if (!silent)
                CommandKeys.LOG.error(
                        LOG_STR,
                        key,
                        cls.getName(),
                        "Value '" + element + "' is not String"
                );
            hasChanged = true;
            return def;
        }

        return element.getAsString();
    }

    /**
     * Integer deserialization helper.
     */
    public static int getOrDefault(JsonObject obj, String key, int def, boolean silent) {
        Class<?> cls = Integer.class;
        JsonElement element = getAsJsonPrimitiveOrNull(obj, key, cls, silent);
        if (element == null)
            return def;

        if (!element.getAsJsonPrimitive().isNumber()) {
            if (!silent)
                CommandKeys.LOG.error(
                        LOG_STR,
                        key,
                        cls.getName(),
                        "Value '" + element + "' is not Number"
                );
            hasChanged = true;
            return def;
        }

        return element.getAsInt();
    }

    /**
     * Float deserialization helper.
     */
    public static float getOrDefault(JsonObject obj, String key, float def, boolean silent) {
        Class<?> cls = Integer.class;
        JsonElement element = getAsJsonPrimitiveOrNull(obj, key, cls, silent);
        if (element == null)
            return def;

        if (!element.getAsJsonPrimitive().isNumber()) {
            if (!silent)
                CommandKeys.LOG.error(
                        LOG_STR,
                        key,
                        cls.getName(),
                        "Value '" + element + "' is not Number"
                );
            hasChanged = true;
            return def;
        }

        return element.getAsFloat();
    }

    /**
     * Boolean deserialization helper.
     */
    public static boolean getOrDefault(JsonObject obj, String key, boolean def, boolean silent) {
        Class<?> cls = Boolean.class;
        JsonElement element = getAsJsonPrimitiveOrNull(obj, key, cls, silent);
        if (element == null)
            return def;

        if (!element.getAsJsonPrimitive().isBoolean()) {
            if (!silent)
                CommandKeys.LOG.error(
                        LOG_STR,
                        key,
                        cls.getName(),
                        "Value '" + element + "' is not Boolean"
                );
            hasChanged = true;
            return def;
        }

        return element.getAsBoolean();
    }

    /**
     * Enum deserialization helper.
     */
    public static <T extends Enum<T>> T getOrDefault(
            JsonObject obj,
            String key,
            Class<T> cls,
            T def,
            boolean silent
    ) {
        JsonElement element = getAsJsonPrimitiveOrNull(obj, key, cls, silent);
        if (element == null)
            return def;

        if (!element.getAsJsonPrimitive().isString()) {
            if (!silent)
                CommandKeys.LOG.error(
                        LOG_STR,
                        key,
                        cls.getName(),
                        "Value '" + element + "' is not String"
                );
            hasChanged = true;
            return def;
        }

        String value = element.getAsString();
        for (T enumVal : cls.getEnumConstants()) {
            if (enumVal.name().equals(value)) {
                return enumVal;
            }
        }

        if (!silent)
            CommandKeys.LOG.error(
                    LOG_STR,
                    key,
                    cls.getName(),
                    "Value '" + value + "' is not in Enum Constants"
            );
        hasChanged = true;
        return def;
    }

    /**
     * {@link InputConstants.Key} deserialization helper.
     */
    public static InputConstants.Key getOrDefault(
            JsonObject obj,
            String key,
            InputConstants.Key def,
            boolean silent
    ) {
        String keyName = getOrDefault(obj, key, (String) null, silent);
        if (keyName == null)
            return def;

        try {
            return InputConstants.getKey(keyName);
        } catch (IllegalArgumentException e) {
            if (!silent)
                CommandKeys.LOG.error(
                        LOG_STR,
                        key,
                        String.class.getName(),
                        "Value '" + keyName + "' is not the name of an InputConstants.Key"
                );
            hasChanged = true;
            return def;
        }
    }

    /**
     * Non-primitive object deserialization helper.
     *
     * <p><b>Note:</b> requires the deserializer for {@code cls} to be
     * registered to the deserializing {@link Gson}.</p>
     */
    public static <T> T getOrDefault(
            JsonDeserializationContext ctx,
            JsonObject obj,
            String key,
            Class<T> cls,
            T def,
            boolean silent
    ) {
        JsonElement element = getAsJsonObjectOrNull(obj, key, cls, silent);
        if (element == null)
            return def;

        return ctx.deserialize(element, cls);
    }

    /**
     * String list deserialization helper.
     */
    public static List<String> getOrDefault(
            JsonObject obj,
            String key,
            List<String> def,
            boolean silent
    ) {
        JsonArray array = getAsJsonArrayOrNull(obj, key, String.class, silent);
        if (array == null)
            return def;

        return array.asList()
                .stream()
                .filter((je) -> (je.isJsonPrimitive() && je.getAsJsonPrimitive().isString()))
                .map(JsonElement::getAsString)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Non-primitive object list deserialization helper.
     *
     * <p><b>Note:</b> requires the deserializer for {@code cls} to be
     * registered to the deserializing {@link Gson}.</p>
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> getOrDefault(
            JsonDeserializationContext ctx,
            JsonObject obj,
            String key,
            Class<T> cls,
            List<T> def,
            boolean silent
    ) {
        JsonArray array = getAsJsonArrayOrNull(obj, key, cls, silent);
        if (array == null)
            return def;

        return array.asList()
                .stream()
                .filter(JsonElement::isJsonObject)
                .map((je) -> (T) ctx.deserialize(je, cls))
                .toList()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
