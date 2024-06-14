/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.config.util;

import com.google.gson.JsonParseException;
import dev.terminalmc.commandkeys.CommandKeys;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class JsonValidator<T> {
    /**
     * Validates a deserialized object.
     * Source: <a href="https://stackoverflow.com/a/21634867">StackOverflow </a>
     * @param obj the object to validate
     * @return the validated object
     * @throws JsonParseException if any {@link JsonRequired} field of the object is
     * {@code null}
     */
    public T validateRequired(T obj) {
        Field[] fields = obj.getClass().getDeclaredFields();
        Method[] methods = obj.getClass().getDeclaredMethods();

        for (Field field : fields) {
            JsonRequired jsonRequired = field.getAnnotation(JsonRequired.class);
            if (jsonRequired != null) {
                try {
                    field.setAccessible(true);
                    if (field.get(obj) == null) {
                        boolean fallbackFound = false;
                        if (!jsonRequired.fallback().isBlank()) {
                            for (Method method : methods) {
                                if (method.getName().equals(jsonRequired.fallback())
                                        && method.getParameterCount() == 0
                                        && method.getReturnType().equals(field.getType())
                                ) {
                                    CommandKeys.LOG.info("Missing field '{}', using fallback '{}'",
                                            field.getName(), method.getName());
                                    method.setAccessible(true);
                                    field.set(obj, method.invoke(obj));
                                    fallbackFound = true;
                                    break;
                                }
                            }
                        }
                        if (!fallbackFound) throw new JsonParseException("Missing field in JSON: " + field.getName());
                    }
                } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                    CommandKeys.LOG.error(e.getMessage());
                }
            }
        }
        return obj;
    }
}
