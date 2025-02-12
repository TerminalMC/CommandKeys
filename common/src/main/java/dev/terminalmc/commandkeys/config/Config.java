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

import com.google.gson.*;
import dev.terminalmc.commandkeys.CommandKeys;
import dev.terminalmc.commandkeys.util.JsonUtil;
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
import java.util.function.Supplier;

import static dev.terminalmc.commandkeys.config.Profile.LINK_PROFILE_MAP;

/**
 * Config consists of a list of {@link Profile} instances, two {@code int}
 * 'pointers' to keep track of the default profiles for singleplayer and
 * multiplayer, default options for new {@link Profile} or {@link Macro}
 * instances, and global mod options.
 *
 * <p>When a profile is activated it is automatically moved to the start of the
 * list, so the list maintains most-recently-used order and the current active
 * profile can be obtained using {@code getFirst()}.</p>
 *
 * <p>The profile list is guaranteed to contain at least one instance at all
 * times, and at least two if {@link Config#spDefault} is not equal to
 * {@link Config#mpDefault}.</p>
 */
public class Config {
    public static final int VERSION = 5;
    public final int version = VERSION;
    private static final Path DIR_PATH = Path.of("config");
    public static final String FILE_NAME = CommandKeys.MOD_ID + ".json";
    public static final String UNREADABLE_FILE_NAME = CommandKeys.MOD_ID + ".unreadable.json";
    public static final String OLD_FILE_NAME = CommandKeys.MOD_ID + ".old.json";
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new Config.Deserializer())
            .registerTypeAdapter(Profile.class, new Profile.Deserializer())
            .registerTypeAdapter(Macro.class, new Macro.Deserializer())
            .registerTypeAdapter(Keybind.class, new Keybind.Deserializer())
            .registerTypeAdapter(Message.class, new Message.Deserializer())
            .setPrettyPrinting()
            .create();

    // Profile list
    private final List<Profile> profiles;
    private static final Supplier<List<Profile>> profilesDefault = 
            () -> new ArrayList<>(List.of(new Profile("Default Profile")));
    
    private int spDefault;
    private int mpDefault;
    public static final int defaultIndexDefault = 0;

    // Default options used by new macro instances
    public Macro.ConflictStrategy defaultConflictStrategy;
    public Macro.SendMode defaultSendMode;

    // Ratelimit options
    private int ratelimitCount;
    public static final int ratelimitCountDefault = 4;
    private int ratelimitTicks;
    public static final int ratelimitTicksDefault = 20;
    public boolean ratelimitStrict;
    public static final boolean ratelimitStrictDefault = false;
    public boolean ratelimitSp;
    public static final boolean ratelimitSpDefault = false;

    /**
     * Creates a profile list with a single profile, set as both singleplayer
     * and multiplayer default.
     */
    public Config() {
        this(
                profilesDefault.get(),
                defaultIndexDefault,
                defaultIndexDefault,
                Macro.ConflictStrategy.values()[0],
                Macro.SendMode.values()[0],
                ratelimitCountDefault,
                ratelimitTicksDefault,
                ratelimitStrictDefault,
                ratelimitSpDefault
        );
    }

    /**
     * Not validated, only for use by default constructor or self-validating
     * deserializer.
     */
    private Config(
            List<Profile> profiles,
            int spDefault,
            int mpDefault, 
            Macro.ConflictStrategy defaultConflictStrategy,
            Macro.SendMode defaultSendMode,
            int ratelimitCount,
            int ratelimitTicks,
            boolean ratelimitStrict,
            boolean ratelimitSp
    ) {
        this.profiles = profiles;
        this.spDefault = spDefault;
        this.mpDefault = mpDefault;
        this.defaultConflictStrategy = defaultConflictStrategy;
        this.defaultSendMode = defaultSendMode;
        this.ratelimitCount = ratelimitCount;
        this.ratelimitTicks = ratelimitTicks;
        this.ratelimitStrict = ratelimitStrict;
        this.ratelimitSp = ratelimitSp;
    }
    
    // Default profile pointer management

    public int getSpDefault() {
        return spDefault;
    }

    public void setSpDefault(int index) {
        if (index < 0 || index >= profiles.size()) 
            throw new IndexOutOfBoundsException(index);
        this.spDefault = index;
    }

    public int getMpDefault() {
        return mpDefault;
    }

    public void setMpDefault(int index) {
        if (spDefault < 0 || spDefault >= profiles.size())
            throw new IndexOutOfBoundsException(index);
        this.mpDefault = index;
    }
    
    // Ratelimit management

    public int getRatelimitCount() {
        return ratelimitCount;
    }

    public void setRatelimitCount(int count) {
        if (count < 1) throw new IllegalArgumentException();
        this.ratelimitCount = count;
    }

    public int getRatelimitTicks() {
        return ratelimitTicks;
    }

    public void setRatelimitTicks(int ticks) {
        if (ticks < 1) throw new IllegalArgumentException();
        this.ratelimitTicks = ticks;
    }
    
    // Profile activation handling

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
            // Stop all repeating macros of active profile (if set to do so)
            profiles.getFirst().getMacros().forEach((macro) -> {
                if (!macro.resumeRepeatingStatus) macro.stopRepeating();
            });
            // Activate requested profile
            profiles.addFirst(profiles.remove(index));
            // Update default pointers
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
    
    // Profile list management

    /**
     * @return an unmodifiable view of the profiles list.
     */
    public List<Profile> getProfiles() {
        return Collections.unmodifiableList(profiles);
    }

    /**
     * Adds a custom copy of {@code profile} (with no links) to the end of the
     * list.
     * 
     * @return the copy {@link Profile}.
     */
    public Profile addCopyProfile(Profile profile) {
        Profile copy = new Profile(profile);
        profiles.add(copy);
        return profile;
    }

    /**
     * Adds a new {@link Profile} to the end of the list.
     * 
     * @return the new {@link Profile}.
     */
    public Profile addNewProfile() {
        Profile profile = new Profile();
        profiles.add(profile);
        return profile;
    }

    /**
     * Removes the {@link Profile} at {@code index} in the list.
     */
    public void removeProfile(int index) {
        Profile profile = profiles.remove(index);
        // Remove links from map
        profile.getLinks().forEach(LINK_PROFILE_MAP::remove);
        // Update default pointers
        if (index < spDefault) spDefault--;
        if (index < mpDefault) mpDefault--;
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

    public static Config reload() {
        instance = null;
        LINK_PROFILE_MAP.clear(); // Only static state
        return get();
    }

    // Load and save

    public static @NotNull Config load() {
        Path file = DIR_PATH.resolve(FILE_NAME);
        Config config = null;
        if (Files.exists(file)) {
            JsonUtil.reset();
            config = load(file, GSON);
            if (config == null) {
                backup(UNREADABLE_FILE_NAME);
                CommandKeys.LOG.warn("Resetting config");
                CommandKeys.hasResetConfig = true;
            } else if (JsonUtil.hasChanged) {
                backup(OLD_FILE_NAME);
            }
        }
        return config != null ? config : new Config();
    }

    private static @Nullable Config load(Path file, Gson gson) {
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file.toFile()), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            // Catch Exception as errors in deserialization may not fall under
            // IOException or JsonParseException, but should not crash the game.
            CommandKeys.LOG.error("Unable to load config", e);
            return null;
        }
    }

    private static void backup(String path) {
        try {
            CommandKeys.LOG.warn("Copying {} to {}", FILE_NAME, path);
            if (!Files.isDirectory(DIR_PATH)) Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path backupFile = file.resolveSibling(path);
            Files.move(file, backupFile, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            CommandKeys.LOG.error("Unable to copy config file", e);
        }
    }

    public static void save() {
        if (instance == null) return;
        instance.validate();
        try {
            if (!Files.isDirectory(DIR_PATH)) Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(tempFile.toFile()), StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(instance));
            } catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            CommandKeys.onConfigSaved(instance);
        } catch (IOException e) {
            CommandKeys.LOG.error("Unable to save config", e);
        }
    }
    
    // Validation

    /**
     * Validation method to be called after config editing and before saving.
     */
    private Config validate() {
        profiles.forEach(Profile::validate);
        
        if (profiles.isEmpty()) profiles.addAll(profilesDefault.get());
        
        if (spDefault < 0 || spDefault >= profiles.size()) spDefault = defaultIndexDefault;
        if (mpDefault < 0 || mpDefault >= profiles.size()) mpDefault = defaultIndexDefault;
        
        if (ratelimitCount < 1) ratelimitCount = ratelimitCountDefault;
        if (ratelimitTicks < 1) ratelimitTicks = ratelimitTicksDefault;

        return this;
    }

    // Deserialization

    public static class Deserializer implements JsonDeserializer<Config> {
        @Override
        public Config deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            int version = obj.has("version") ? obj.get("version").getAsInt() : 0;
            boolean silent = version != VERSION;

            Macro.ConflictStrategy defaultConflictStrategy = JsonUtil.getOrDefault(obj, "defaultConflictStrategy",
                    Macro.ConflictStrategy.class, Macro.conflictStrategyDefault, silent);

            Macro.SendMode defaultSendMode = JsonUtil.getOrDefault(obj, "defaultSendMode",
                    Macro.SendMode.class, Macro.sendModeDefault, silent);
            
            int ratelimitCount = JsonUtil.getOrDefault(obj, "ratelimitCount",
                    ratelimitCountDefault, silent);

            int ratelimitTicks = JsonUtil.getOrDefault(obj, "ratelimitTicks",
                    ratelimitTicksDefault, silent);
        
            boolean ratelimitStrict = JsonUtil.getOrDefault(obj, "ratelimitStrict",
                    ratelimitStrictDefault, silent);

            boolean ratelimitSp = JsonUtil.getOrDefault(obj, "ratelimitSp",
                    ratelimitSpDefault, silent);

            List<Profile> profiles = JsonUtil.getOrDefault(ctx, obj, "profiles",
                    Profile.class, profilesDefault.get(), silent);
            
            int spDefault = JsonUtil.getOrDefault(obj, "spDefault",
                    defaultIndexDefault, silent);
            
            int mpDefault = JsonUtil.getOrDefault(obj, "mpDefault",
                    defaultIndexDefault, silent);
            
            return new Config(
                    profiles,
                    spDefault,
                    mpDefault, 
                    defaultConflictStrategy,
                    defaultSendMode,
                    ratelimitCount,
                    ratelimitTicks,
                    ratelimitStrict,
                    ratelimitSp
            ).validate();
        }
    }
}
