/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.config;

import com.google.common.collect.HashMultimap;
import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;

import java.lang.reflect.Type;
import java.util.*;

public class Profile {
    public final int version = 1;

    public static final Map<String, Profile> WORLD_PROFILE_MAP = new HashMap<>();

    public transient final HashMultimap<InputConstants.Key, CommandKey> commandKeyMap = HashMultimap.create();

    public String name;
    private final Set<String> addresses;

    public boolean addToHistory;
    public boolean showHudMessage;
    private final Set<CommandKey> commandKeys;

    public Profile() {
        this.name = "";
        this.addresses = new LinkedHashSet<>();
        this.addToHistory = false;
        this.showHudMessage = false;
        this.commandKeys = new LinkedHashSet<>();
    }

    public Profile(String name, Set<String> addresses, boolean addToHistory,
                   boolean showHudMessage, Set<CommandKey> commandKeys) {
        this.name = name;
        this.addresses = addresses;
        this.addToHistory = addToHistory;
        this.showHudMessage = showHudMessage;
        this.commandKeys = commandKeys;

        Iterator<String> addressIter = this.addresses.iterator();
        while(addressIter.hasNext()) {
            String address = addressIter.next();
            if (WORLD_PROFILE_MAP.containsKey(address)) addressIter.remove();
            else WORLD_PROFILE_MAP.put(address, this);
        }
    }

    public Profile(Profile profile) {
        this.name = profile.name;
        this.addresses = new HashSet<>();
        this.addToHistory = profile.addToHistory;
        this.showHudMessage = profile.showHudMessage;
        this.commandKeys = profile.commandKeys;
    }

    public String getDisplayName() {
        String name = this.name;
        if (name.isBlank()) name = this.getLinks().stream().findFirst().orElse("");
        if (name.isBlank()) name = "[Unnamed]";
        return name;
    }

    public Set<String> getLinks() {
        return Collections.unmodifiableSet(addresses);
    }

    public void addAddress(String address) {
        if (WORLD_PROFILE_MAP.containsKey(address)) return;
        addresses.add(address);
        WORLD_PROFILE_MAP.put(address, this);
    }

    public void forceAddAddress(String address) {
        if (WORLD_PROFILE_MAP.containsKey(address)) WORLD_PROFILE_MAP.get(address).removeAddress(address);
        addAddress(address);
    }

    public void removeAddress(String address) {
        addresses.remove(address);
        WORLD_PROFILE_MAP.remove(address);
    }

    public Set<CommandKey> getCmdKeys() {
        return Collections.unmodifiableSet(commandKeys);
    }

    public void addCmdKey(CommandKey cmdKey) {
        commandKeys.add(cmdKey);
    }

    public void removeCmdKey(CommandKey cmdKey) {
        commandKeys.remove(cmdKey);
        commandKeyMap.remove(cmdKey.getKey(), cmdKey);
    }

    // Cleanup and validation

    public void cleanup() {
        Iterator<CommandKey> cmdKeyIter = commandKeys.iterator();
        while (cmdKeyIter.hasNext()) {
            CommandKey cmk = cmdKeyIter.next();
            // Allow blank messages for cycling command keys as spacers
            switch(cmk.sendStrategy.state) {
                case ZERO -> {
                    cmk.messages.replaceAll(String::stripTrailing);
                    cmk.messages.removeIf(String::isBlank);
                }
                case TWO -> cmk.messages.replaceAll(String::stripTrailing);
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
            Set<String> addresses = new HashSet<>();
            for (JsonElement je : obj.getAsJsonArray("addresses")) addresses.add(je.getAsString());
            boolean addToHistory = obj.get("addToHistory").getAsBoolean();
            boolean showHudMessage = obj.get("showHudMessage").getAsBoolean();

            // Deserialize CommandKey objects with link to deserialized Profile
            Set<CommandKey> commandKeys = new LinkedHashSet<>();

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
