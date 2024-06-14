/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.commandkeys.config;

import com.google.gson.*;
import dev.terminalmc.commandkeys.CommandKeys;
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
    public final int version = 1;
    private static final Path DIR_PATH = Path.of("config");
    private static final String FILE_NAME = CommandKeys.MOD_ID + ".json";
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new Serializer())
            .registerTypeAdapter(Config.class, new Deserializer())
            .registerTypeAdapter(Profile.class, new Profile.Serializer())
            .registerTypeAdapter(Profile.class, new Profile.Deserializer())
            .registerTypeAdapter(CommandKey.class, new CommandKey.Serializer())
            .setPrettyPrinting()
            .create();

    private transient Profile activeProfile;

    // Options

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
            profiles.addFirst(temp);
            // If active was the old default, make active the new default.
            if (activeProfile.equals(temp)) activeProfile = profile;
        }
    }

    public void setMpDefaultProfile(@NotNull Profile profile) {
        if (!profile.equals(mpDefaultProfile)) {
            Profile temp = mpDefaultProfile;
            mpDefaultProfile = profile;
            profiles.remove(profile);
            profiles.addFirst(temp);
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

    // Instance management

    private static Config instance = null;

    public static Config get() {
        if (instance == null) {
            instance = Config.load();
        }
        return instance;
    }

    public static Config getAndSave() {
        get();
        save();
        return instance;
    }

    public static Config resetAndSave() {
        instance = new Config();
        save();
        return instance;
    }


    // Load and save

    public static @NotNull Config load() {
        Path file = DIR_PATH.resolve(FILE_NAME);
        Config config = null;
        if (Files.exists(file)) {
            config = load(file, GSON);
        }
        if (config == null) {
            config = new Config();
        }
        return config;
    }

    private static @Nullable Config load(Path file, Gson gson) {
        try (FileReader reader = new FileReader(file.toFile())) {
            return gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            // Catch Exception as errors in deserialization may not fall under
            // IOException or JsonParseException, but should not crash the game.
            CommandKeys.LOG.error("Unable to load config.", e);
            return null;
        }
    }

    public static void save() {
        try {
            if (!Files.isDirectory(DIR_PATH)) Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");

            try (FileWriter writer = new FileWriter(tempFile.toFile())) {
                writer.write(GSON.toJson(instance));
            } catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            CommandKeys.onConfigSaved(instance);
        } catch (IOException e) {
            CommandKeys.LOG.error("Unable to save config.", e);
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
