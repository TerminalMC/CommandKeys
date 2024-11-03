/*
 * Copyright 2024 TerminalMC
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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Consists of behavioral options, a list of {@link Macro} instances, and
 * a list of strings identifying worlds and/or servers to which the
 * {@link Profile} is linked, collectively referred to as 'links'.
 *
 * <p>A static {@link Map} {@link Profile#LINK_PROFILE_MAP} is maintained to
 * ensure no overlap of links across different profiles, and to improve link
 * lookup time.</p>
 *
 * <p>A transient {@link Multimap} {@link Profile#keyMacroMap} is maintained
 * to improve keybind lookup time.</p>
 */
public class Profile {
    public final int version = 2;

    public enum Control {
        ON,
        OFF,
        DEFER
    }

    public static final Map<String, Profile> LINK_PROFILE_MAP = new HashMap<>();

    public transient final Multimap<InputConstants.Key, Macro> keyMacroMap
            = LinkedHashMultimap.create();

    public String name;
    private final List<String> addresses;

    public Control addToHistory;
    public Control showHudMessage;

    private final List<Macro> macros;

    /**
     * Creates a default empty instance.
     */
    public Profile() {
        this.name = "";
        this.addresses = new ArrayList<>();
        this.addToHistory = Control.OFF;
        this.showHudMessage = Control.OFF;
        this.macros = new ArrayList<>();
    }

    /**
     * Not validated, only for use by self-validating deserializer.
     */
    private Profile(String name, List<String> addresses, Control addToHistory,
                    Control showHudMessage, List<Macro> macros) {
        this.name = name;
        this.addresses = addresses;
        this.addToHistory = addToHistory;
        this.showHudMessage = showHudMessage;
        this.macros = macros;

        // Add links to map, those that already exist are removed from local
        Iterator<String> linkIter = this.addresses.iterator();
        while(linkIter.hasNext()) {
            String link = linkIter.next();
            if (LINK_PROFILE_MAP.containsKey(link)) linkIter.remove();
            else LINK_PROFILE_MAP.put(link, this);
        }
    }

    /**
     * Copy constructor.
     */
    public Profile(Profile profile) {
        this.name = profile.name;
        this.addresses = new ArrayList<>();
        this.addToHistory = profile.addToHistory;
        this.showHudMessage = profile.showHudMessage;
        this.macros = profile.macros;
    }

    /**
     * @return the first non-blank of the following: the profile name, the first
     * link, the string "[Unnamed]".
     */
    public String getDisplayName() {
        String name = this.name;
        if (name.isBlank()) name = this.getLinks().stream().findFirst().orElse("");
        if (name.isBlank()) name = "[Unnamed]";
        return name;
    }

    /**
     * @return an unmodifiable view of the link list.
     */
    public List<String> getLinks() {
        return Collections.unmodifiableList(addresses);
    }

    /**
     * Adds the link to this profile and to {@link Profile#LINK_PROFILE_MAP},
     * after removing it from any other profile.
     */
    public void forceAddLink(String link) {
        if (LINK_PROFILE_MAP.containsKey(link)) LINK_PROFILE_MAP.get(link).removeLink(link);
        addresses.add(link);
        LINK_PROFILE_MAP.put(link, this);
    }

    /**
     * Removes the link from this profile and from
     * {@link Profile#LINK_PROFILE_MAP}.
     */
    public void removeLink(String link) {
        addresses.remove(link);
        LINK_PROFILE_MAP.remove(link);
    }

    /**
     * @return an unmodifiable view of the {@link Macro} list.
     */
    public List<Macro> getMacros() {
        return Collections.unmodifiableList(macros);
    }

    /**
     * Adds {@code macro} to the {@link Macro} list and to
     * {@link Profile#keyMacroMap}.
     */
    public void addMacro(Macro macro) {
        macros.add(macro);
        keyMacroMap.put(macro.getKey(), macro);
    }

    /**
     * Moves the {@link Macro} at the source index to the destination
     * index.
     * @param sourceIndex the index of the element to move.
     * @param destIndex the desired final index of the element.
     */
    public void moveMacro(int sourceIndex, int destIndex) {
        if (sourceIndex != destIndex) {
            macros.add(destIndex, macros.remove(sourceIndex));
            rebuildMacroMap();
        }
    }

    /**
     * Removes {@code macro} from the {@link Macro} list and from
     * {@link Profile#keyMacroMap}.
     */
    public void removeMacro(Macro macro) {
        macros.remove(macro);
        keyMacroMap.remove(macro.getKey(), macro);
    }

    /**
     * Clears {@link Profile#keyMacroMap}, then adds each element of the
     * {@link Macro} list, in order.
     */
    public void rebuildMacroMap() {
        keyMacroMap.clear();
        for (Macro macro : macros) {
            keyMacroMap.put(macro.getKey(), macro);
        }
    }

    // Cleanup and validation

    public void cleanup() {
        Iterator<Macro> macroIter = macros.iterator();
        while (macroIter.hasNext()) {
            Macro macro = macroIter.next();
            // Allow blank messages for cycling command keys as spacers
            if (!macro.getSendMode().equals(Macro.SendMode.TYPE)) {
                macro.messages.forEach((msg) -> msg.string = msg.string.stripTrailing());
            }
            if (!macro.getSendMode().equals(Macro.SendMode.CYCLE)) {
                macro.messages.removeIf((msg) -> msg.string.isBlank());
            }
            if (macro.messages.isEmpty()) {
                macroIter.remove();
                keyMacroMap.remove(macro.getKey(), macro);
            }
        }
    }

    // Deserialization

    public static class Deserializer implements JsonDeserializer<Profile> {
        @Override
        public Profile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            int version = obj.has("version") ? obj.get("version").getAsInt() : 0;

            String name = obj.get("name").getAsString();
            List<String> addresses = new ArrayList<>();
            for (JsonElement je : obj.getAsJsonArray("addresses")) addresses.add(je.getAsString());
            Control addToHistory = version >= 2
                    ? Control.valueOf(obj.get("addToHistory").getAsString())
                    : Control.OFF;
            Control showHudMessage = version >= 2
                    ? Control.valueOf(obj.get("showHudMessage").getAsString())
                    : Control.OFF;

            // Deserialize CommandKey objects with link to deserialized Profile
            List<Macro> macros = new ArrayList<>();

            Profile profile = new Profile(name, addresses, addToHistory, showHudMessage, macros);

            Gson macroGson = new GsonBuilder()
                    .registerTypeAdapter(Macro.class, new Macro.Deserializer(profile))
                    .create();

            for (JsonElement je : obj.getAsJsonArray(version >= 2 ? "macros" : "commandKeys"))
                macros.add(macroGson.fromJson(je, Macro.class));

            // Validate
            if (name == null) throw new JsonParseException("Profile #1");

            return profile;
        }
    }
}
