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
import java.util.List;

import static dev.terminalmc.commandkeys.config.Profile.WORLD_PROFILE_MAP;

public class Config {
    public final int version = 2;
    private static final Path DIR_PATH = Path.of("config");
    private static final String FILE_NAME = CommandKeys.MOD_ID + ".json";
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new Config.Deserializer())
            .registerTypeAdapter(Profile.class, new Profile.Deserializer())
            .registerTypeAdapter(CommandKey.class, new CommandKey.Serializer())
            .setPrettyPrinting()
            .create();

    // Options

    public final List<Profile> profiles;
    public int spDefault;
    public int mpDefault;

    public Config() {
        this.profiles = new ArrayList<>();

        Profile spDefaultProfile = new Profile();
        spDefaultProfile.name = "Singleplayer Default";
        this.profiles.add(spDefaultProfile);

        Profile mpDefaultProfile = new Profile();
        mpDefaultProfile.name = "Singleplayer Default";
        this.profiles.add(mpDefaultProfile);
    }

    public Config(List<Profile> profiles, int spDefault, int mpDefault) {
        this.profiles = profiles;
        this.spDefault = spDefault;
        this.mpDefault = mpDefault;
        activateProfile(spDefault);
    }

    public Profile activeProfile() {
        return profiles.getFirst();
    }

    public void activateProfile(int index) {
        if (index != 0) {
            profiles.addFirst(profiles.remove(index));
            if (index == spDefault) spDefault = 0;
            else if (index > spDefault) spDefault++;
            if (index == mpDefault) mpDefault = 0;
            else if (index > mpDefault) mpDefault++;
        }
    }

    public void setSpDefaultProfile(int index) {
        spDefault = index;
    }

    public void setMpDefaultProfile(int index) {
        mpDefault = index;
    }

    public void copyProfile(Profile profile) {
        Profile copyProfile = new Profile(profile);
        copyProfile.name = profile.getDisplayName() + " (Copy)";
        profiles.add(copyProfile);
    }

    public void activateSpProfile(String levelId) {
        if (WORLD_PROFILE_MAP.containsKey(levelId)) {
            int i = 0;
            for (Profile profile : profiles) {
                for (String id : profile.getLinks()) {
                    if (id.equals(levelId)) {
                        activateProfile(i);
                        return;
                    }
                }
                i++;
            }
        }
        activateProfile(spDefault);
    }

    public void activateMpProfile(String address) {
        if (WORLD_PROFILE_MAP.containsKey(address)) {
            int i = 0;
            for (Profile profile : profiles) {
                for (String id : profile.getLinks()) {
                    if (id.equals(address)) {
                        activateProfile(i);
                        return;
                    }
                }
                i++;
            }
        }
        activateProfile(mpDefault);
    }

    // Cleanup

    public void cleanup() {
        for (Profile p : profiles) p.cleanup();
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
        instance.cleanup();
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

    // Deserialization

    public static class Deserializer implements JsonDeserializer<Config> {
        @Override
        public Config deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            int version = obj.has("version") ? obj.get("version").getAsInt() : 0;

            List<Profile> profiles = new ArrayList<>();
            for (JsonElement je : obj.getAsJsonArray("profiles")) {
                profiles.add(ctx.deserialize(je, Profile.class));
            }
            int spDefault;
            int mpDefault;

            if (version == 1) {
                profiles.addFirst(ctx.deserialize(obj.get("mpDefaultProfile"), Profile.class));
                profiles.addFirst(ctx.deserialize(obj.get("spDefaultProfile"), Profile.class));
                if (profiles.size() < 2) throw new JsonParseException(
                        "Expected 2 or more profiles, got " + profiles.size());
                spDefault = 0;
                mpDefault = 1;
            }
            else {
                spDefault = obj.get("spDefault").getAsInt();
                mpDefault = obj.get("mpDefault").getAsInt();
            }

            // Validate
            if (spDefault < 0 || spDefault >= profiles.size()) throw new JsonParseException("Config #1");
            if (mpDefault < 0 || mpDefault >= profiles.size()) throw new JsonParseException("Config #2");

            return new Config(profiles, spDefault, mpDefault);
        }
    }
}
