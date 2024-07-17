/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.config;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Consists of behavioral options, a list of {@link CommandKey} instances, and
 * a list of strings identifying worlds and/or servers to which the
 * {@link Profile} is linked, collectively referred to as 'links'.
 *
 * <p>A static {@link Map} {@link Profile#LINK_PROFILE_MAP} is maintained to
 * ensure no overlap of links across different profiles, and to improve link
 * lookup time.</p>
 *
 * <p>A transient {@link Multimap} {@link Profile#commandKeyMap} is maintained
 * to improve keybind lookup time.</p>
 */
public class Profile {
    public final int version = 1;

    public static final Map<String, Profile> LINK_PROFILE_MAP = new HashMap<>();

    public transient final Multimap<InputConstants.Key, CommandKey> commandKeyMap
            = LinkedHashMultimap.create();

    public String name;
    private final List<String> addresses;

    public boolean addToHistory;
    public boolean showHudMessage;
    private final List<CommandKey> commandKeys;

    /**
     * Creates a default empty instance.
     */
    public Profile() {
        this.name = "";
        this.addresses = new ArrayList<>();
        this.addToHistory = false;
        this.showHudMessage = false;
        this.commandKeys = new ArrayList<>();
    }

    /**
     * Not validated, only for use by self-validating deserializer.
     */
    private Profile(String name, List<String> addresses, boolean addToHistory,
                   boolean showHudMessage, List<CommandKey> commandKeys) {
        this.name = name;
        this.addresses = addresses;
        this.addToHistory = addToHistory;
        this.showHudMessage = showHudMessage;
        this.commandKeys = commandKeys;

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
        this.commandKeys = profile.commandKeys;
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
     * @return an unmodifiable view of the {@link CommandKey} list.
     */
    public List<CommandKey> getCmdKeys() {
        return Collections.unmodifiableList(commandKeys);
    }

    /**
     * Adds {@code cmdKey} to the {@link CommandKey} list and to
     * {@link Profile#commandKeyMap}.
     */
    public void addCmdKey(CommandKey cmdKey) {
        commandKeys.add(cmdKey);
        commandKeyMap.put(cmdKey.getKey(), cmdKey);
    }

    /**
     * Moves the {@link CommandKey} at the source index to the destination
     * index.
     * @param sourceIndex the index of the element to move.
     * @param destIndex the desired final index of the element.
     */
    public void moveCmdKey(int sourceIndex, int destIndex) {
        if (sourceIndex != destIndex) {
            commandKeys.add(destIndex, commandKeys.remove(sourceIndex));
            rebuildCmdKeyMap();
        }
    }

    /**
     * Removes {@code cmdKey} from the {@link CommandKey} list and from
     * {@link Profile#commandKeyMap}.
     */
    public void removeCmdKey(CommandKey cmdKey) {
        commandKeys.remove(cmdKey);
        commandKeyMap.remove(cmdKey.getKey(), cmdKey);
    }

    /**
     * Clears {@link Profile#commandKeyMap}, then adds each element of the
     * {@link CommandKey} list, in order.
     */
    public void rebuildCmdKeyMap() {
        commandKeyMap.clear();
        for (CommandKey cmdKey : commandKeys) {
            commandKeyMap.put(cmdKey.getKey(), cmdKey);
        }
    }

    // Cleanup and validation

    public void cleanup() {
        Iterator<CommandKey> cmdKeyIter = commandKeys.iterator();
        while (cmdKeyIter.hasNext()) {
            CommandKey cmk = cmdKeyIter.next();
            // Allow blank messages for cycling command keys as spacers
            switch(cmk.sendStrategy.state) {
                case ZERO -> {
                    cmk.messages.removeIf((msg) -> msg.string.isBlank());
                    cmk.messages.forEach((msg) -> msg.string = msg.string.stripTrailing());
                }
                case TWO -> cmk.messages.forEach((msg) -> msg.string = msg.string.stripTrailing());
            }
            if (cmk.messages.isEmpty()) {
                cmdKeyIter.remove();
                commandKeyMap.remove(cmk.getKey(), cmk);
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
            boolean addToHistory = obj.get("addToHistory").getAsBoolean();
            boolean showHudMessage = obj.get("showHudMessage").getAsBoolean();

            // Deserialize CommandKey objects with link to deserialized Profile
            List<CommandKey> commandKeys = new ArrayList<>();

            Profile profile = new Profile(name, addresses, addToHistory, showHudMessage, commandKeys);

            Gson commandKeyGson = new GsonBuilder()
                    .registerTypeAdapter(CommandKey.class, new CommandKey.Deserializer(profile))
                    .create();

            for (JsonElement je : obj.getAsJsonArray("commandKeys"))
                commandKeys.add(commandKeyGson.fromJson(je, CommandKey.class));

            // Validate
            if (name == null) throw new JsonParseException("Profile #1");

            return profile;
        }
    }
}
