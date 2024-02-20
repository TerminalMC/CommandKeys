package com.notryken.commandkeys.config;

import com.google.common.collect.HashMultimap;
import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;

import java.lang.reflect.Type;
import java.util.*;

public class Profile {
    public static final Map<String, Profile> PROFILE_MAP = new HashMap<>();

    public transient final HashMultimap<InputConstants.Key, CommandKey> COMMANDKEY_MAP = HashMultimap.create();

    public String name;
    private final Set<String> addresses;

    public boolean addToHistory;
    public boolean showHudMessage;
    private final Set<CommandKey> commandKeys;

    public Profile() {
        this.name = "";
        this.addresses = new HashSet<>();
        this.addToHistory = false;
        this.showHudMessage = false;
        this.commandKeys = new LinkedHashSet<>();
    }

    public Profile(String name, Set<String> addresses,
                   boolean addToHistory, boolean showHudMessage,
                   Set<CommandKey> commandKeys) {
        this.name = name;
        this.addresses = addresses;
        this.addToHistory = addToHistory;
        this.showHudMessage = showHudMessage;
        this.commandKeys = commandKeys;

        Iterator<String> addressIter = this.addresses.iterator();
        while(addressIter.hasNext()) {
            String address = addressIter.next();
            if (PROFILE_MAP.containsKey(address)) addressIter.remove();
            else PROFILE_MAP.put(address, this);
        }
    }

    public Profile(Profile profile) {
        this.name = profile.name;
        this.addresses = new HashSet<>();
        this.addToHistory = profile.addToHistory;
        this.showHudMessage = profile.showHudMessage;
        this.commandKeys = profile.commandKeys;
    }

    public Set<String> getAddresses() {
        return Collections.unmodifiableSet(addresses);
    }

    public boolean addAddress(String address) {
        if (PROFILE_MAP.containsKey(address)) return false;
        addresses.add(address);
        PROFILE_MAP.put(address, this);
        return true;
    }

    public void forceAddAddress(String address) {
        if (PROFILE_MAP.containsKey(address)) PROFILE_MAP.get(address).removeAddress(address);
        addAddress(address);
    }

    public void removeAddress(String address) {
        addresses.remove(address);
        PROFILE_MAP.remove(address);
    }

    public Set<CommandKey> getCmdKeys() {
        return Collections.unmodifiableSet(commandKeys);
    }

    public void addCmdKey(CommandKey cmdKey) {
        commandKeys.add(cmdKey);
    }

    public void removeCmdKey(CommandKey cmdKey) {
        commandKeys.remove(cmdKey);
        COMMANDKEY_MAP.remove(cmdKey.getKey(), cmdKey);
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
                COMMANDKEY_MAP.remove(cmk.getKey(), cmk);
            }
        }
    }

    // Serialization / Deserialization

    public static class Serializer implements JsonSerializer<Profile> {
        @Override
        public JsonElement serialize(Profile src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject profileObj = new JsonObject();

            profileObj.addProperty("name", src.name);
            profileObj.add("addresses", context.serialize(src.addresses));
            
            profileObj.addProperty("addToHistory", src.addToHistory);
            profileObj.addProperty("showHudMessage", src.showHudMessage);
            profileObj.add("commandKeys", context.serialize(src.commandKeys));

            return profileObj;
        }
    }

    public static class Deserializer implements JsonDeserializer<Profile> {
        @Override
        public Profile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            JsonObject profileObj = json.getAsJsonObject();

            String name;
            Set<String> addresses = new HashSet<>();

            boolean addToHistory;
            boolean showHudMessage;
            Set<CommandKey> commandKeys = new LinkedHashSet<>();

            name = profileObj.get("name").getAsString();
            JsonArray addressesArr = profileObj.getAsJsonArray("addresses");
            for (JsonElement element : addressesArr) {
                addresses.add(element.getAsString());
            }
            
            addToHistory = profileObj.get("addToHistory").getAsBoolean();
            showHudMessage = profileObj.get("showHudMessage").getAsBoolean();

            // Deserialize CommandKey objects with link to deserialized Profile

            Profile profile = new Profile(name, addresses, addToHistory, showHudMessage, commandKeys);
            Gson commandKeyGson = new GsonBuilder()
                    .registerTypeAdapter(CommandKey.class, new CommandKey.Deserializer(profile))
                    .create();

            JsonArray commandKeysArr = profileObj.getAsJsonArray("commandKeys");
            for (JsonElement element : commandKeysArr) {
                commandKeys.add(commandKeyGson.fromJson(element, CommandKey.class));
            }

            return profile;
        }
    }
}
