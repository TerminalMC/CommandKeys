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
import java.util.Collections;
import java.util.List;

import static dev.terminalmc.commandkeys.config.Profile.LINK_PROFILE_MAP;

/**
 * Config consists of a list of {@link Profile} instances, and two {@code int}s
 * to keep track of the default profiles for singleplayer and multiplayer.</p>
 *
 * <p>When a profile is activated it is automatically moved to the start of the
 * list, so the list maintains most-recently-used order and the current active
 * profile can be obtained using {@code getFirst()}.</p>
 *
 * <p>The profile list is guaranteed to contain at least one instance at all
 * times, and at least two if the singleplayer default instance is not also the
 * multiplayer default instance.</p>
 */
public class Config {
    public final int version = 3;
    private static final Path DIR_PATH = Path.of("config");
    private static final String FILE_NAME = CommandKeys.MOD_ID + ".json";
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new Config.Deserializer())
            .registerTypeAdapter(Profile.class, new Profile.Deserializer())
            .registerTypeAdapter(CommandKey.class, new CommandKey.Serializer())
            .registerTypeAdapter(Message.class, new Message.Deserializer())
            .setPrettyPrinting()
            .create();

    // Options

    public final QuadState defaultConflictStrategy;
    public final TriState defaultSendMode;

    private final List<Profile> profiles;
    public int spDefault;
    public int mpDefault;

    /**
     * Creates a profile list with two profiles, one as singleplayer default and
     * the other as multiplayer default.
     */
    public Config() {
        this.defaultConflictStrategy = new QuadState();
        this.defaultSendMode = new TriState();

        this.profiles = new ArrayList<>();

        Profile spDefaultProfile = new Profile();
        spDefaultProfile.name = "Profile 1";
        this.profiles.add(spDefaultProfile);
        this.spDefault = 0;

        Profile mpDefaultProfile = new Profile();
        mpDefaultProfile.name = "Profile 2";
        this.profiles.add(mpDefaultProfile);
        this.mpDefault = 1;
    }

    /**
     * Not validated, only for use by self-validating deserializer.
     */
    private Config(QuadState defaultConflictStrategy, TriState defaultSendMode,
                   List<Profile> profiles, int spDefault, int mpDefault) {
        this.defaultConflictStrategy = defaultConflictStrategy;
        this.defaultSendMode = defaultSendMode;
        this.profiles = profiles;
        this.spDefault = spDefault;
        this.mpDefault = mpDefault;
        activateProfile(spDefault);
    }

    /**
     * @return the most recently activated {@link Profile}.
     */
    public Profile activeProfile() {
        return profiles.getFirst();
    }

    /**
     * Activates the {@link Profile} at {@code index}, if it is not already
     * active.
     */
    public void activateProfile(int index) {
        if (index != 0) {
            profiles.addFirst(profiles.remove(index));
            if (index == spDefault) spDefault = 0;
            else if (index > spDefault) spDefault++;
            if (index == mpDefault) mpDefault = 0;
            else if (index > mpDefault) mpDefault++;
        }
    }

    /**
     * Activates the profile linked to the level ID, if one exists, else
     * activates the singleplayer default profile.
     */
    public void activateSpProfile(String levelId) {
        Profile profile = LINK_PROFILE_MAP.getOrDefault(levelId, null);
        if (profile != null) {
            activateProfile(profiles.indexOf(profile));
        } else {
            activateProfile(spDefault);
        }
    }

    /**
     * Activates the profile linked to the address, if one exists, else
     * activates the multiplayer default profile.
     */
    public void activateMpProfile(String address) {
        Profile profile = LINK_PROFILE_MAP.getOrDefault(address, null);
        if (profile != null) {
            activateProfile(profiles.indexOf(profile));
        } else {
            activateProfile(mpDefault);
        }
    }

    /**
     * @return an unmodifiable view of the profiles list.
     */
    public List<Profile> getProfiles() {
        return Collections.unmodifiableList(profiles);
    }

    /**
     * Creates an exact copy of the {@code profile}, minus links and with
     * " (Copy)" appended to the name.
     */
    public void copyProfile(Profile profile) {
        Profile copyProfile = new Profile(profile);
        copyProfile.name = profile.getDisplayName() + " (Copy)";
        profiles.add(copyProfile);
    }

    /**
     * Adds {@code profile} to the {@link Profile} list.
     */
    public void addProfile(Profile profile) {
        profiles.add(profile);
    }

    /**
     * Removes the element at {@code index} in the {@link Profile} list.
     */
    public void removeProfile(int index) {
        profiles.remove(index);
        if (index < spDefault) spDefault--;
        if (index < mpDefault) mpDefault--;
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

            QuadState defaultConflictStrategy = version >= 3
                    ? new QuadState(obj.getAsJsonObject("defaultConflictStrategy")
                            .get("state").getAsString())
                    : new QuadState();
            TriState defaultSendMode = version >= 3
                    ? new TriState(obj.getAsJsonObject("defaultSendMode")
                            .get("state").getAsString())
                    : new TriState();

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
            } else {
                spDefault = obj.get("spDefault").getAsInt();
                mpDefault = obj.get("mpDefault").getAsInt();
            }

            // Validate
            if (spDefault < 0 || spDefault >= profiles.size()) throw new JsonParseException("Config #1");
            if (mpDefault < 0 || mpDefault >= profiles.size()) throw new JsonParseException("Config #2");

            return new Config(defaultConflictStrategy, defaultSendMode, profiles, spDefault, mpDefault);
        }
    }
}
