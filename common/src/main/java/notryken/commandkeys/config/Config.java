package notryken.commandkeys.config;

import com.google.gson.*;
import notryken.commandkeys.CommandKeys;
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
import java.util.Iterator;
import java.util.List;

public class Config {
    // Constants
    public static final String DEFAULT_FILE_NAME = "commandkeys.json";
    private static final Gson CONFIG_GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new Config.Serializer())
            .registerTypeAdapter(Config.class, new Config.Deserializer())
            .registerTypeAdapter(Profile.class, new Profile.Serializer())
            .registerTypeAdapter(Profile.class, new Profile.Deserializer())
            .registerTypeAdapter(CommandKey.class, new CommandKey.Serializer())
            .registerTypeAdapter(CommandKey.class, new CommandKey.Deserializer())
            .registerTypeAdapter(KeyPair.class, new KeyPair.Serializer())
            .registerTypeAdapter(KeyPair.class, new KeyPair.Deserializer())
            .setPrettyPrinting()
            .create();

    // Not saved, not modifiable by user
    private static Path configPath;

    // Not saved, modifiable by user
    private Profile activeProfile;

    // Saved, not modifiable by user
    // 1 is initial version following switch from commandkeys_v1.json
    private final int version = 1;

    // Saved, modifiable by user
    private Profile spDefaultProfile;
    private final ArrayList<Profile> spProfiles;
    private Profile mpDefaultProfile;
    private final ArrayList<Profile> mpProfiles;

    public Config() {
        this.spDefaultProfile = new Profile(true);
        this.spProfiles = new ArrayList<>();
        this.mpDefaultProfile = new Profile(false);
        this.mpProfiles = new ArrayList<>();
        this.activeProfile = this.mpDefaultProfile;
    }

    public Config(Profile spDefaultProfile, ArrayList<Profile> spProfiles,
                  Profile mpDefaultProfile, ArrayList<Profile> mpProfiles) {
        this.spDefaultProfile = spDefaultProfile;
        this.spProfiles = spProfiles;
        this.mpDefaultProfile = mpDefaultProfile;
        this.mpProfiles = mpProfiles;
        this.activeProfile = this.mpDefaultProfile;
    }


    public Profile getActiveProfile() {
        return activeProfile;
    }

    public void setActiveProfile(Profile activeProfile) {
        this.activeProfile = activeProfile;
    }



    public Profile getSpDefaultProfile() {
        return spDefaultProfile;
    }

    public List<Profile> getSpProfiles() {
        return Collections.unmodifiableList(spProfiles);
    }

    public void addSpProfile() {
        spProfiles.add(new Profile(false));
    }

    public void addSpProfile(Profile profile) {
        spProfiles.add(profile);
    }

    public Profile getMpDefaultProfile() {
        return mpDefaultProfile;
    }

    public List<Profile> getMpProfiles() {
        return Collections.unmodifiableList(mpProfiles);
    }

    public void addMpProfile() {
        mpProfiles.add(new Profile(false));
    }

    public void addMpProfile(Profile profile) {
        mpProfiles.add(profile);
    }


    public void removeProfile(Profile profile) {
        if (profile.singleplayer) {
            spProfiles.remove(profile);
        }
        else {
            mpProfiles.remove(profile);
        }
    }

    public void setAsDefault(Profile profile) {
        if (profile.singleplayer) {
            if (!profile.equals(spDefaultProfile)) {
                Profile temp = spDefaultProfile;
                spDefaultProfile = profile;
                spProfiles.remove(profile);
                spProfiles.add(0, temp);
                if (activeProfile.equals(temp)) activeProfile = profile;
            }
        }
        else {
            if (!profile.equals(mpDefaultProfile)) {
                Profile temp = mpDefaultProfile;
                mpDefaultProfile = profile;
                mpProfiles.remove(profile);
                mpProfiles.add(0, temp);
                if (activeProfile.equals(temp)) activeProfile = profile;
            }
        }
    }

    public void copyProfile(Profile profile) {
        if (profile.singleplayer) {
            spProfiles.add(new Profile(profile));
        }
        else {
            mpProfiles.add(new Profile(profile));
        }
    }


    // Load and save

    public static @NotNull Config load() {
        long time = System.currentTimeMillis();
        CommandKeys.LOG.info("CommandKeys: Loading config from file...");

        Config config = load(DEFAULT_FILE_NAME, CONFIG_GSON);

        if (config == null) {
            CommandKeys.LOG.info("CommandKeys: Using default configuration");
            config = new Config();
        }

        configPath = Path.of("config").resolve(DEFAULT_FILE_NAME);
        config.writeToFile();
        CommandKeys.LOG.info("CommandKeys: Configuration loaded in {} ms",
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
                CommandKeys.LOG.warn("CommandKeys: Unable to load config from file '{}'. Reason:", name, e);
            }
        }
        else {
            CommandKeys.LOG.warn("CommandKeys: Unable to locate config file '{}'", name);
        }
        return config;
    }

    /**
     * Writes the config to the global configPath.
     */
    public void writeToFile() {
        long time = System.currentTimeMillis();
        CommandKeys.LOG.info("CommandKeys: Saving config to file...");

        spDefaultProfile.cleanup();
        Iterator<Profile> iter = spProfiles.iterator();
        while (iter.hasNext()) {
            Profile profile = iter.next();
            profile.cleanup();
            if (profile.getCommandKeys().isEmpty()) iter.remove();
        }

        mpDefaultProfile.cleanup();
        iter = mpProfiles.iterator();
        while (iter.hasNext()) {
            Profile profile = iter.next();
            profile.cleanup();
            if (profile.getCommandKeys().isEmpty()) iter.remove();
        }

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

            CommandKeys.LOG.info("CommandKeys: Configuration saved in {} ms",
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
            configObj.add("spProfiles", context.serialize(src.spProfiles));
            configObj.add("mpDefaultProfile", context.serialize(src.mpDefaultProfile));
            configObj.add("mpProfiles", context.serialize(src.mpProfiles));

            return configObj;
        }
    }

    public static class Deserializer implements JsonDeserializer<Config> {
        @Override
        public Config deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject configObj = json.getAsJsonObject();

            Profile spDefaultProfile;
            ArrayList<Profile> spProfiles = new ArrayList<>();
            Profile mpDefaultProfile;
            ArrayList<Profile> mpProfiles = new ArrayList<>();

            JsonObject spDefaultProfileObj = configObj.getAsJsonObject("spDefaultProfile");
            spDefaultProfile = context.deserialize(spDefaultProfileObj, Profile.class);

            JsonArray spProfilesArr = configObj.getAsJsonArray("spProfiles");
            for (JsonElement element : spProfilesArr) {
                spProfiles.add(context.deserialize(element, Profile.class));
            }

            JsonObject mdDefaultProfileObj = configObj.getAsJsonObject("mpDefaultProfile");
            mpDefaultProfile = context.deserialize(mdDefaultProfileObj, Profile.class);

            JsonArray mpProfilesArr = configObj.getAsJsonArray("mpProfiles");
            for (JsonElement element : mpProfilesArr) {
                mpProfiles.add(context.deserialize(element, Profile.class));
            }

            return new Config(spDefaultProfile, spProfiles, mpDefaultProfile, mpProfiles);
        }
    }
}
