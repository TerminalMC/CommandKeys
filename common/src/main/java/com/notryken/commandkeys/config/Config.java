/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package com.notryken.commandkeys.config;

import com.google.gson.*;
import com.notryken.commandkeys.CommandKeys;
import com.notryken.commandkeys.config.util.GhettoAsciiWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

public class Config {
    // Constants
    public static final String DEFAULT_FILE_NAME = "commandkeys.json";
    private static final Gson CONFIG_GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new Config.Serializer())
            .registerTypeAdapter(Config.class, new Config.Deserializer())
            .registerTypeAdapter(Profile.class, new Profile.Serializer())
            .registerTypeAdapter(Profile.class, new Profile.Deserializer())
            .registerTypeAdapter(CommandKey.class, new CommandKey.Serializer())
            .setPrettyPrinting()
            .create();

    // Not saved, not modifiable by user
    private static Path configPath;

    // Not saved, modifiable by user
    private transient Profile activeProfile;

    // Saved, not modifiable by user
    private final int version = 1;

    // Saved, modifiable by user
    private Profile spDefaultProfile;
    private Profile mpDefaultProfile;
    public final ArrayList<Profile> profiles;

    public Config() {
        Profile spDefaultProfile = new Profile();
        spDefaultProfile.name = "Singleplayer Default";
        this.spDefaultProfile = spDefaultProfile;

        Profile mpDefaultProfile = new Profile();
        mpDefaultProfile.name = "Multiplayer Default";
        this.mpDefaultProfile = mpDefaultProfile;

        this.profiles = new ArrayList<>();
        this.activeProfile = this.mpDefaultProfile;
    }

    public Config(@NotNull Profile spDefaultProfile, @NotNull Profile mpDefaultProfile,
                  ArrayList<Profile> profiles) {
        this.spDefaultProfile = spDefaultProfile;
        this.mpDefaultProfile = mpDefaultProfile;
        this.profiles = profiles;
        this.activeProfile = this.mpDefaultProfile;
    }


    public @NotNull Profile getActiveProfile() {
        return activeProfile;
    }

    public void setActiveProfile(@NotNull Profile activeProfile) {
        this.activeProfile = activeProfile;
    }

    public @NotNull Profile getSpDefaultProfile() {
        return spDefaultProfile;
    }

    public @NotNull Profile getMpDefaultProfile() {
        return mpDefaultProfile;
    }

    public void setSpDefaultProfile(@NotNull Profile profile) {
        if (!profile.equals(spDefaultProfile)) {
            Profile temp = spDefaultProfile;
            spDefaultProfile = profile;
            profiles.remove(profile);
            profiles.add(0, temp);
            // If active was the old default, make active the new default.
            if (activeProfile.equals(temp)) activeProfile = profile;
        }
    }

    public void setMpDefaultProfile(@NotNull Profile profile) {
        if (!profile.equals(mpDefaultProfile)) {
            Profile temp = mpDefaultProfile;
            mpDefaultProfile = profile;
            profiles.remove(profile);
            profiles.add(0, temp);
            // If active was the old default, make active the new default.
            if (activeProfile.equals(temp)) activeProfile = profile;
        }
    }

    public void copyProfile(Profile profile) {
        Profile copyProfile = new Profile(profile);
        copyProfile.name = copyProfile.name + " (Copy)";
        profiles.add(copyProfile);
    }

    // Cleanup

    public void cleanup() {
        spDefaultProfile.cleanup();
        mpDefaultProfile.cleanup();

        for (Profile profile : profiles) {
            profile.cleanup();
        }
    }

    // Load and save

    public static @NotNull Config load() {
        long time = System.currentTimeMillis();
        CommandKeys.LOG.info("Loading config from file...");

        Config config = load(DEFAULT_FILE_NAME, CONFIG_GSON);

        if (config == null) {
            CommandKeys.LOG.info("Using default configuration");
            config = new Config();
        }

        configPath = Path.of("config").resolve(DEFAULT_FILE_NAME);
        config.writeToFile();
        CommandKeys.LOG.info("Configuration loaded in {} ms",
                System.currentTimeMillis() - time);
        return config;
    }

    public static @Nullable Config load(String name, Gson gson) {
        configPath = Path.of("config").resolve(name);
        Config config = null;

        if (Files.exists(configPath)) {
            try (FileReader reader = new FileReader(configPath.toFile())) {
                config = gson.fromJson(reader, Config.class);
            } catch (Exception e) {
                CommandKeys.LOG.warn("Unable to load config from file '{}'. Reason:", name, e);
            }
        }
        else {
            CommandKeys.LOG.warn("Unable to locate config file '{}'", name);
        }
        return config;
    }

    /**
     * Writes the config to the global configPath.
     */
    public void writeToFile() {
        long time = System.currentTimeMillis();
        CommandKeys.LOG.info("Saving config to file...");

        cleanup();

        Path dir = configPath.getParent();

        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            else if (!Files.isDirectory(dir)) {
                throw new IOException("Not a directory: " + dir);
            }

            // Use a temporary location next to the config's final destination
            Path tempPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");

            // Write the file to the temporary location
            FileWriter out = new FileWriter(tempPath.toFile());
            CONFIG_GSON.toJson(this, new GhettoAsciiWriter(out));
            out.close();

            // Atomically replace the old config file (if it exists) with the temporary file
            Files.move(tempPath, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

            CommandKeys.LOG.info("Configuration saved in {} ms",
                    System.currentTimeMillis() - time);
        }
        catch (IOException e) {
            throw new RuntimeException("CommandKeys: Unable to update config file. Reason:", e);
        }
    }

    // Serialization / Deserialization

    public static class Serializer implements JsonSerializer<Config> {
        @Override
        public JsonElement serialize(Config src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject configObj = new JsonObject();
            
            configObj.addProperty("version", src.version);
            configObj.add("spDefaultProfile", context.serialize(src.spDefaultProfile));
            configObj.add("mpDefaultProfile", context.serialize(src.mpDefaultProfile));
            configObj.add("profiles", context.serialize(src.profiles));

            return configObj;
        }
    }

    public static class Deserializer implements JsonDeserializer<Config> {
        @Override
        public Config deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject configObj = json.getAsJsonObject();

            Profile spDefaultProfile;
            Profile mpDefaultProfile;
            ArrayList<Profile> profiles = new ArrayList<>();

            JsonObject spDefaultProfileObj = configObj.getAsJsonObject("spDefaultProfile");
            spDefaultProfile = context.deserialize(spDefaultProfileObj, Profile.class);

            JsonObject mdDefaultProfileObj = configObj.getAsJsonObject("mpDefaultProfile");
            mpDefaultProfile = context.deserialize(mdDefaultProfileObj, Profile.class);

            JsonArray mpProfilesArr = configObj.getAsJsonArray("profiles");
            for (JsonElement element : mpProfilesArr) {
                profiles.add(context.deserialize(element, Profile.class));
            }

            return new Config(spDefaultProfile, mpDefaultProfile, profiles);
        }
    }
}
