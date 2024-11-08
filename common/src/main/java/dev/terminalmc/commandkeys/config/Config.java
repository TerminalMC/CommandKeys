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

import com.google.gson.*;
import dev.terminalmc.commandkeys.CommandKeys;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dev.terminalmc.commandkeys.config.Profile.LINK_PROFILE_MAP;

/**
 * Config consists of a list of {@link Profile} instances, two {@code int}s to 
 * keep track of the default profiles for singleplayer and multiplayer, and one
 * or more default options for new {@link Profile} or {@link Macro} instances.
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
    public final int version = 4;
    private static final Path DIR_PATH = Path.of("config");
    private static final String FILE_NAME = CommandKeys.MOD_ID + ".json";
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new Config.Deserializer())
            .registerTypeAdapter(Profile.class, new Profile.Deserializer())
            .registerTypeAdapter(Message.class, new Message.Deserializer())
            .setPrettyPrinting()
            .create();

    // Default options used by new macro instances
    public Macro.ConflictStrategy defaultConflictStrategy;
    public Macro.SendMode defaultSendMode;

    // Profile list
    private final List<Profile> profiles;
    public int spDefault;
    public int mpDefault;

    /**
     * Creates a profile list with a single profile, set as both singleplayer
     * and multiplayer default.
     */
    public Config() {
        this.defaultConflictStrategy = Macro.ConflictStrategy.SUBMIT;
        this.defaultSendMode = Macro.SendMode.SEND;

        this.profiles = new ArrayList<>();

        Profile defaultProfile = new Profile();
        defaultProfile.name = "Default Profile";
        this.profiles.add(defaultProfile);
        this.spDefault = 0;
        this.mpDefault = 0;
    }

    /**
     * Not validated, only for use by self-validating deserializer.
     */
    private Config(Macro.ConflictStrategy defaultConflictStrategy, Macro.SendMode defaultSendMode,
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
        profiles.getFirst().getMacros().forEach(Macro::stopRepeating);
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
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8)) {
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

            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile.toFile()), StandardCharsets.UTF_8)) {
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

            Macro.ConflictStrategy defaultConflictStrategy = version >= 4
                    ? Macro.ConflictStrategy.valueOf(obj.get("defaultConflictStrategy").getAsString())
                    : Macro.ConflictStrategy.SUBMIT;
            Macro.SendMode defaultSendMode = version >= 4
                    ? Macro.SendMode.valueOf(obj.get("defaultSendMode").getAsString())
                    : Macro.SendMode.SEND;

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
            if (profiles.isEmpty()) throw new JsonParseException("Config Error: profiles.isEmpty()");
            if (spDefault < 0 || spDefault >= profiles.size()) spDefault = 0;
            if (mpDefault < 0 || mpDefault >= profiles.size()) mpDefault = 0;

            return new Config(defaultConflictStrategy, defaultSendMode, profiles, spDefault, mpDefault);
        }
    }
}
