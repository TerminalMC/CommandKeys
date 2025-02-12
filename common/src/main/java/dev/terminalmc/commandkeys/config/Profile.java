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
import dev.terminalmc.commandkeys.util.JsonUtil;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Consists of behavioral options, a list of {@link Macro} instances, and a list
 * of strings identifying worlds and/or servers to which the profile is linked,
 * collectively referred to as 'links'.
 *
 * <p>A static {@link Map} {@link Profile#LINK_PROFILE_MAP} is maintained to
 * ensure no overlap of links across different profiles, and to improve link
 * lookup time. Note that as such, no two profiles can be allowed to contain
 * the same link.</p>
 *
 * <p>A pair of transient {@link Multimap} instances ({@link Profile#keybindMap}
 * and {@link Profile#macroMap}) are maintained to improve macro lookup time.
 * </p>
 */
public class Profile {
    public static final int VERSION = 4;
    public final int version = VERSION;
    
    public static final Map<String, Profile> LINK_PROFILE_MAP = new HashMap<>();
    
    public transient final Multimap<InputConstants.Key, Keybind> keybindMap 
            = LinkedHashMultimap.create();
    public transient final Multimap<Keybind, Macro> macroMap 
            = LinkedHashMultimap.create();

    // Profile details
    public String name;
    public static final String nameDefault = "";
    private final List<String> links;
    public static final Supplier<List<String>> linksDefault = ArrayList::new;

    // Behavior controls
    private Control addToHistory;
    public static final Control addToHistoryDefault = Control.OFF;
    private Control showHudMessage;
    public static final Control showHudMessageDefault = Control.OFF;
    private Control resumeRepeating;
    public static final Control resumeRepeatingDefault = Control.OFF;
    private Control useRatelimit;
    public static final Control useRatelimitDefault = Control.ON;
    public enum Control {
        ON,
        OFF,
        DEFER
    }

    // Macro list
    private final List<Macro> macros;
    public static final Supplier<List<Macro>> macrosDefault = ArrayList::new;

    /**
     * Creates a default empty instance.
     */
    public Profile() {
        this(nameDefault);
    }
    
    public Profile(String name) {
        this(
                name,
                linksDefault.get(),
                addToHistoryDefault,
                showHudMessageDefault,
                resumeRepeatingDefault,
                useRatelimitDefault,
                macrosDefault.get()
        );
    }

    /**
     * Not validated, only for use by self-validating deserializer.
     */
    private Profile(
            String name,
            List<String> links,
            Control addToHistory,
            Control showHudMessage,
            Control resumeRepeating,
            Control useRatelimit,
            List<Macro> macros
    ) {
        this.name = name;
        this.links = links;
        this.addToHistory = addToHistory;
        this.showHudMessage = showHudMessage;
        this.resumeRepeating = resumeRepeating;
        this.useRatelimit = useRatelimit;
        this.macros = macros;
        // Add missing links to map, and remove from the links list any that
        // were already present in the map.
        this.links.removeIf((link) -> LINK_PROFILE_MAP.putIfAbsent(link, this) != null);
    }

    /**
     * Custom copy constructor.
     * 
     * <p><b>Note:</b> all fields are copied except for {@link Profile#name}
     * (which has is set to the value of {@link Profile#getDisplayName()} with
     * {@code " (Copy)"} appended), and {@link Profile#links} (which is set to
     * default).</p>
     */
    Profile(Profile profile) {
        this.name = profile.getDisplayName() + " (Copy)";
        this.links = linksDefault.get();
        this.addToHistory = profile.addToHistory;
        this.showHudMessage = profile.showHudMessage;
        this.resumeRepeating = profile.resumeRepeating;
        this.useRatelimit = profile.useRatelimit;
        this.macros = profile.macros.stream().map(Macro::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }
    
    // Display name util
    
    /**
     * @return the first non-blank of the following: {@link Profile#name}, 
     * the first element of {@link Profile#links}, the string 
     * {@code "[Unnamed]"}.
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

    public Control getResumeRepeating() {
        return resumeRepeating;
    }

    public void setResumeRepeating(Control resumeRepeating) {
        this.resumeRepeating = resumeRepeating;
        macros.forEach((macro) -> setResumeRepeating(macro, macro.resumeRepeating));
    }

    public Control getUseRatelimit() {
        return useRatelimit;
    }

    public void setUseRatelimit(Control useRatelimit) {
        this.useRatelimit = useRatelimit;
        macros.forEach((macro) -> setUseRatelimit(macro, macro.useRatelimit));
    }
    
    // Macro management

    /**
     * @return an unmodifiable view of the {@link Macro} list.
     */
    public List<Macro> getMacros() {
        return Collections.unmodifiableList(macros);
    }

    /**
     * Adds {@code macro} to this profile.
     */
    public void addMacro(Macro macro) {
        macros.add(macro);
        addToMaps(macro);
    }

    /**
     * Removes {@code macro} from this profile.
     */
    public void removeMacro(Macro macro) {
        macros.remove(macro);
        rebuildMaps();
    }

    /**
     * Moves the {@link Macro} at the source index to the destination index.
     * @param sourceIndex the index of the element to move.
     * @param destIndex the desired final index of the element.
     */
    public boolean moveMacro(int sourceIndex, int destIndex) {
        if (sourceIndex != destIndex) {
            macros.add(destIndex, macros.remove(sourceIndex));
            rebuildMaps();
            return true;
        }
        return false;
    }
    
    // Macro map management

    /**
     * Adds the keybind key and, if appropriate, the alternate keybind key of 
     * {@code macro} to {@link Profile#keybindMap}, and adds {@code macro} to
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
        // Rebuilding maps is required as only certain types of macro use their
        // alternate keybind.
        rebuildMaps();
    }
    
    public void setConflictStrategy(Macro macro, Macro.ConflictStrategy conflictStrategy) {
        if (conflictStrategy.equals(macro.conflictStrategy)) return;
        macro.clearScheduled();
        macro.conflictStrategy = conflictStrategy;
    }

    /**
     * If {@code keybind} is the {@link Macro#keybind} or
     * {@link Macro#altKeybind} of {@code macro}, sets the primary key of the
     * appropriate {@link Keybind} to {@code key}.
     */
    public void setKey(Macro macro, Keybind keybind, InputConstants.Key key) {
        if (key.equals(keybind.getKey())) return;
        if (keybind == macro.keybind || keybind == macro.altKeybind) {
            macro.clearScheduled();
            keybind.setKey(key);
            rebuildMaps();
        }
    }

    /**
     * If {@code keybind} is the {@link Macro#keybind} or
     * {@link Macro#altKeybind} of {@code macro}, sets the limit key of the
     * appropriate {@link Keybind} to {@code key}.
     */
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
        macro.addToHistoryStatus = switch(this.addToHistory) {
            case ON -> true;
            case OFF -> false;
            case DEFER -> macro.addToHistory;
        };
    }

    public void setShowHudMessage(Macro macro, boolean value) {
        macro.showHudMessage = value;
        macro.showHudMessageStatus = switch(this.showHudMessage) {
            case ON -> true;
            case OFF -> false;
            case DEFER -> macro.showHudMessage;
        };
    }

    public void setResumeRepeating(Macro macro, boolean value) {
        macro.resumeRepeating = value;
        macro.resumeRepeatingStatus = switch(this.resumeRepeating) {
            case ON -> true;
            case OFF -> false;
            case DEFER -> macro.resumeRepeating;
        };
    }

    public void setUseRatelimit(Macro macro, boolean value) {
        macro.useRatelimit = value;
        macro.useRatelimitStatus = switch(this.useRatelimit) {
            case ON -> true;
            case OFF -> false;
            case DEFER -> macro.useRatelimit;
        };
    }

    // Validation

    Profile validate() {
        macros.forEach(Macro::validate);
        macros.removeIf((macro) -> macro.messages.isEmpty());
        
        // Update transients in macros
        setAddToHistory(addToHistory);
        setShowHudMessage(showHudMessage);
        setResumeRepeating(resumeRepeating);
        setUseRatelimit(useRatelimit);
        
        // Possibly not required?
        rebuildMaps();
        
        return this;
    }
    
    // Deserialization

    public static class Deserializer implements JsonDeserializer<Profile> {
        @Override
        public Profile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            int version = obj.has("version") ? obj.get("version").getAsInt() : 0;
            boolean silent = version != VERSION;

            String name = JsonUtil.getOrDefault(obj, "name",
                    nameDefault, silent);
            
            List<String> addresses = JsonUtil.getOrDefault(obj, "links", 
                    JsonUtil.getOrDefault(obj, "addresses",
                            new ArrayList<>(), silent),
                    silent);
            
            Control addToHistory = JsonUtil.getOrDefault(obj, "addToHistory",
                    Control.class, addToHistoryDefault, silent);

            Control showHudMessage = JsonUtil.getOrDefault(obj, "showHudMessage",
                    Control.class, showHudMessageDefault, silent);

            Control resumeRepeating = JsonUtil.getOrDefault(obj, "resumeRepeating",
                    Control.class, resumeRepeatingDefault, silent);

            Control useRatelimit = JsonUtil.getOrDefault(obj, "useRatelimit",
                    Control.class, useRatelimitDefault, silent);
            
            List<Macro> macros = JsonUtil.getOrDefault(ctx, obj, "macros",
                    Macro.class,
                    JsonUtil.getOrDefault(ctx, obj, "commandKeys",
                            Macro.class, new ArrayList<>(), silent),
                    silent);

            return new Profile(
                    name,
                    addresses,
                    addToHistory,
                    showHudMessage,
                    resumeRepeating,
                    useRatelimit,
                    macros
            ).validate();
        }
    }
}
