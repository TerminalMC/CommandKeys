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
 * <p>A pair of transient {@link Multimap} instances ({@link Profile#keybindMap}
 * and {@link Profile#macroMap}) are maintained to improve macro lookup time.
 * </p>
 */
public class Profile {
    public final int version = 3;
    
    public static final Map<String, Profile> LINK_PROFILE_MAP = new HashMap<>();
    
    public transient final Multimap<InputConstants.Key, Keybind> keybindMap 
            = LinkedHashMultimap.create();
    public transient final Multimap<Keybind, Macro> macroMap 
            = LinkedHashMultimap.create();

    // Profile details
    public String name;
    private final List<String> links;

    // Behavior controls
    private Control addToHistory;
    private Control showHudMessage;
    public enum Control {
        ON,
        OFF,
        DEFER
    }

    // Macro list
    private final List<Macro> macros;

    /**
     * Creates a default empty instance.
     */
    public Profile() {
        this("", new ArrayList<>(), Control.OFF, Control.OFF, new ArrayList<>());
    }
    
    public Profile(String name) {
        this(name, new ArrayList<>(), Control.OFF, Control.OFF, new ArrayList<>());
    }

    /**
     * Not validated, only for use by self-validating deserializer.
     */
    private Profile(String name, List<String> links, Control addToHistory, 
                    Control showHudMessage, List<Macro> macros) {
        this.name = name;
        this.links = links;
        this.addToHistory = addToHistory;
        this.showHudMessage = showHudMessage;
        this.macros = macros;
        // Add missing links to map
        this.links.removeIf((link) -> LINK_PROFILE_MAP.putIfAbsent(link, this) != null);
    }

    /**
     * Copy constructor.
     */
    Profile(Profile profile) {
        this.name = profile.name;
        this.links = new ArrayList<>();
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
    
    // Link management

    /**
     * @return an unmodifiable view of the link list.
     */
    public List<String> getLinks() {
        return Collections.unmodifiableList(links);
    }

    /**
     * Adds the link to this profile and to {@link Profile#LINK_PROFILE_MAP},
     * after removing it from any other profile.
     */
    public void forceAddLink(String link) {
        if (LINK_PROFILE_MAP.containsKey(link)) LINK_PROFILE_MAP.get(link).removeLink(link);
        links.add(link);
        LINK_PROFILE_MAP.put(link, this);
    }

    /**
     * Removes the link from this profile and from
     * {@link Profile#LINK_PROFILE_MAP}.
     */
    public void removeLink(String link) {
        links.remove(link);
        LINK_PROFILE_MAP.remove(link);
    }
    
    // Behavior management

    public Control getAddToHistory() {
        return addToHistory;
    }

    public void setAddToHistory(Control addToHistory) {
        this.addToHistory = addToHistory;
        macros.forEach((macro) -> setAddToHistory(macro, macro.addToHistory));
    }

    public Control getShowHudMessage() {
        return showHudMessage;
    }

    public void setShowHudMessage(Control showHudMessage) {
        this.showHudMessage = showHudMessage;
        macros.forEach((macro) -> setShowHudMessage(macro, macro.showHudMessage));
    }
    
    // Macro management

    /**
     * @return an unmodifiable view of the {@link Macro} list.
     */
    public List<Macro> getMacros() {
        return Collections.unmodifiableList(macros);
    }
    
    public void addMacro(Macro macro) {
        macros.add(macro);
        addToMaps(macro);
    }

    /**
     * Moves the {@link Macro} at the source index to the destination index.
     * @param sourceIndex the index of the element to move.
     * @param destIndex the desired final index of the element.
     */
    public void moveMacro(int sourceIndex, int destIndex) {
        if (sourceIndex != destIndex) {
            macros.add(destIndex, macros.remove(sourceIndex));
            rebuildMaps();
        }
    }
    
    public void removeMacro(Macro macro) {
        macros.remove(macro);
        rebuildMaps();
    }
    
    // Macro map management

    /**
     * Adds the keybind key and, if appropriate, the alternate keybind key of 
     * {@code macro} to {@link Profile#keybindMap}, and adds the macro to
     * {@link Profile#macroMap}. 
     */
    public void addToMaps(Macro macro) {
        keybindMap.put(macro.keybind.getKey(), macro.keybind);
        macroMap.put(macro.keybind, macro);
        if (macro.usesAltKeybind()) {
            keybindMap.put(macro.altKeybind.getKey(), macro.altKeybind);
            macroMap.put(macro.altKeybind, macro);
        }
    }

    /**
     * Clears and repopulates {@link Profile#keybindMap} and 
     * {@link Profile#macroMap}.
     */
    public void rebuildMaps() {
        keybindMap.clear();
        macroMap.clear();
        for (Macro macro : macros) {
            addToMaps(macro);
        }
    }
    
    // Macro editing
    
    public void setSendMode(Macro macro, Macro.SendMode sendMode) {
        if (sendMode.equals(macro.sendMode)) return;
        macro.clearScheduled();
        macro.sendMode = sendMode;
        rebuildMaps();
    }
    
    public void setConflictStrategy(Macro macro, Macro.ConflictStrategy conflictStrategy) {
        if (conflictStrategy.equals(macro.conflictStrategy)) return;
        macro.clearScheduled();
        macro.conflictStrategy = conflictStrategy;
    }
    
    public void setKey(Macro macro, Keybind keybind, InputConstants.Key key) {
        if (key.equals(keybind.getKey())) return;
        if (keybind == macro.keybind || keybind == macro.altKeybind) {
            macro.clearScheduled();
            keybind.setKey(key);
            rebuildMaps();
        }
    }

    public void setLimitKey(Macro macro, Keybind keybind, InputConstants.Key key) {
        if (key.equals(keybind.getLimitKey())) return;
        if (keybind == macro.keybind || keybind == macro.altKeybind) {
            macro.clearScheduled();
            keybind.setLimitKey(key);
            rebuildMaps();
        }
    }
    
    public void setAddToHistory(Macro macro, boolean value) {
        macro.addToHistory = value;
        macro.historyEnabled = switch(this.addToHistory) {
            case ON -> true;
            case OFF -> false;
            case DEFER -> macro.addToHistory;
        };
    }

    public void setShowHudMessage(Macro macro, boolean value) {
        macro.showHudMessage = value;
        macro.hudMessageEnabled = switch(this.showHudMessage) {
            case ON -> true;
            case OFF -> false;
            case DEFER -> macro.showHudMessage;
        };
    }

    // Cleanup and validation

    void cleanup() {
        macros.removeIf((macro) -> {
            // Allow trailing whitespace only for TYPE mode
            if (!macro.sendMode.equals(Macro.SendMode.TYPE)) {
                macro.messages.forEach((msg) -> msg.string = msg.string.stripTrailing());
            }
            // Allow blank messages for CYCLE mode as spacers and TYPE mode to open chat
            if (!macro.sendMode.equals(Macro.SendMode.CYCLE) && 
                    !macro.sendMode.equals(Macro.SendMode.TYPE)) {
                macro.messages.removeIf((msg) -> msg.string.isBlank());
            }
            return macro.messages.isEmpty();
        });
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
            for (JsonElement je : obj.getAsJsonArray(version >= 3 ? "links" : "addresses")) {
                addresses.add(je.getAsString());
            }
            Control addToHistory = version >= 2
                    ? Control.valueOf(obj.get("addToHistory").getAsString())
                    : Control.OFF;
            Control showHudMessage = version >= 2
                    ? Control.valueOf(obj.get("showHudMessage").getAsString())
                    : Control.OFF;

            // Deserialize CommandKey objects with link to deserialized Profile
            List<Macro> macros = new ArrayList<>();

            Profile profile = new Profile(name, addresses, addToHistory, showHudMessage, macros);
            for (JsonElement je : obj.getAsJsonArray(version >= 2 ? "macros" : "commandKeys")) {
                macros.add(ctx.deserialize(je, Macro.class));
            }
            profile.rebuildMaps();

            // Validate
            if (name == null) throw new JsonParseException("Profile Error: name == null");

            return profile;
        }
    }
}
