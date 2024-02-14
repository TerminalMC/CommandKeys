package notryken.commandkeys.config;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
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
import java.util.*;

/**
 * CommandKeys configuration options class.
 *
 * <p>Includes derivative work of code used by
 * <a href="https://github.com/CaffeineMC/sodium-fabric/">Sodium</a></p>
 */
public class Config {
    // Constants
    public static final String DEFAULT_FILE_NAME = "commandkeys.json";
    public static final String LEGACY_FILE_NAME = "commandkeys_v1.json";
    private static final Gson CONFIG_GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new Config.Serializer())
            .registerTypeAdapter(Config.class, new Config.Deserializer())
            .registerTypeAdapter(CommandMonoKey.class, new CommandMonoKey.Serializer())
            .registerTypeAdapter(CommandMonoKey.class, new CommandMonoKey.Deserializer())
            .registerTypeAdapter(CommandDualKey.class, new CommandDualKey.Serializer())
            .registerTypeAdapter(CommandDualKey.class, new CommandDualKey.Deserializer())
            .setPrettyPrinting()
            .create();
    private static final Gson LEGACY_CONFIG_GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new Config.LegacyDeserializer())
            .create();

    // Not saved, not modifiable by user
    private static Path configPath;
    public static boolean configChecked;

    // Saved, not modifiable by user
    // 1 is initial version following switch from commandkeys_v1.json
    private final int version = 1;

    // Saved, modifiable by user
    public boolean monoAddToHistory;
    public boolean monoShowHudMessage;
    private final Set<CommandMonoKey> monoKeySet;
    public boolean dualAddToHistory;
    public boolean dualShowHudMessage;
    private final Set<CommandDualKey> dualKeySet;

    public Config() {
        monoAddToHistory = false;
        monoShowHudMessage = false;
        monoKeySet = new LinkedHashSet<>();
        dualAddToHistory = false;
        dualShowHudMessage = false;
        dualKeySet = new LinkedHashSet<>();
    }

    public Config(boolean monoAddToHistory, boolean monoShowHudMessage, Set<CommandMonoKey> monoKeySet,
                  boolean dualAddToHistory, boolean dualShowHudMessage, Set<CommandDualKey> dualKeySet) {
        this.monoAddToHistory = monoAddToHistory;
        this.monoShowHudMessage = monoShowHudMessage;
        this.monoKeySet = monoKeySet;
        this.dualAddToHistory = dualAddToHistory;
        this.dualShowHudMessage = dualShowHudMessage;
        this.dualKeySet = dualKeySet;
    }

    public Set<CommandMonoKey> getMonoKeySet() {
        return Collections.unmodifiableSet(monoKeySet);
    }

    public void addMonoKey(CommandMonoKey monoKey) {
        monoKeySet.add(monoKey);
    }

    public void removeMonoKey(CommandMonoKey monoKey) {
        monoKeySet.remove(monoKey);
        CommandMonoKey.MAP.remove(monoKey.getKey());
    }

    public Set<CommandDualKey> getDualKeySet() {
        return Collections.unmodifiableSet(dualKeySet);
    }

    public void addDualKey(CommandDualKey dualKey) {
        dualKeySet.add(dualKey);
    }

    public void removeDualKey(CommandDualKey dualKey) {
        dualKeySet.remove(dualKey);
        CommandDualKey.MAP.remove(dualKey.getKey());
    }

    // Cleanup and validation

    public void cleanup() {
        Iterator<CommandMonoKey> monoKeyIter = monoKeySet.iterator();
        while (monoKeyIter.hasNext()) {
            CommandMonoKey cmk = monoKeyIter.next();
            // Allow blank messages for cycling command keys as spacers
            if (!cmk.cycle) cmk.messages.removeIf(String::isBlank);
            if (cmk.fullSend) cmk.messages.replaceAll(String::stripTrailing);
            if (cmk.messages.isEmpty()) monoKeyIter.remove();
        }

        Iterator<CommandDualKey> dualKeyIter = dualKeySet.iterator();
        while (dualKeyIter.hasNext()) {
            CommandDualKey cdk = dualKeyIter.next();
            cdk.messages.removeIf(String::isBlank);
            if (cdk.messages.isEmpty()) dualKeyIter.remove();
        }
    }

    // Load and save

    public static @NotNull Config load() {
        long time = System.currentTimeMillis();
        CommandKeys.LOG.info("CommandKeys: Loading config from file...");

        Config config = load(DEFAULT_FILE_NAME, CONFIG_GSON);

        if (config == null) {
            CommandKeys.LOG.info("CommandKeys: Loading legacy config from file...");
            config = load(LEGACY_FILE_NAME, LEGACY_CONFIG_GSON);
        }

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
            
            configObj.addProperty("monoAddToHistory", src.monoAddToHistory);
            configObj.addProperty("monoShowHudMessage", src.monoShowHudMessage);
            JsonElement monoKeySetElement = context.serialize(src.monoKeySet);
            configObj.add("monoKeySet", monoKeySetElement);

            configObj.addProperty("dualAddToHistory", src.dualAddToHistory);
            configObj.addProperty("dualShowHudMessage", src.dualShowHudMessage);
            JsonElement dualKeySetElement = context.serialize(src.dualKeySet);
            configObj.add("dualKeySet", dualKeySetElement);

            return configObj;
        }
    }

    public static class Deserializer implements JsonDeserializer<Config> {
        @Override
        public Config deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject configObj = json.getAsJsonObject();

            boolean monoAddToHistory;
            boolean monoShowHudMessage;
            Set<CommandMonoKey> monoKeySet = new LinkedHashSet<>();
            boolean dualAddToHistory;
            boolean dualShowHudMessage;
            Set<CommandDualKey> dualKeySet = new LinkedHashSet<>();

            monoAddToHistory = configObj.get("monoAddToHistory").getAsBoolean();
            monoShowHudMessage = configObj.get("monoShowHudMessage").getAsBoolean();
            JsonArray monoKeySetArr = configObj.getAsJsonArray("monoKeySet");
            for (JsonElement element : monoKeySetArr) {
                monoKeySet.add(context.deserialize(element, CommandMonoKey.class));
            }

            dualAddToHistory = configObj.get("dualAddToHistory").getAsBoolean();
            dualShowHudMessage = configObj.get("dualShowHudMessage").getAsBoolean();
            JsonArray dualKeySetArr = configObj.getAsJsonArray("dualKeySet");
            for (JsonElement element : dualKeySetArr) {
                dualKeySet.add(context.deserialize(element, CommandDualKey.class));
            }

            return new Config(monoAddToHistory, monoShowHudMessage, monoKeySet, 
                    dualAddToHistory, dualShowHudMessage, dualKeySet);
        }
    }

    public static class LegacyDeserializer implements JsonDeserializer<Config> {
        @Override
        public Config deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject configObj = json.getAsJsonObject();

            boolean addToHistory;
            boolean showHudMessage;
            Set<CommandMonoKey> monoKeySet = new LinkedHashSet<>();
            Set<CommandDualKey> dualKeySet = new LinkedHashSet<>();

            addToHistory = configObj.get("addToHistory").getAsBoolean();
            showHudMessage = configObj.get("showHudMessage").getAsBoolean();

            JsonArray monoKeySetArr = configObj.getAsJsonArray("msgKeyListMono");
            for (JsonElement element : monoKeySetArr) {
                JsonObject monoKeyObj = element.getAsJsonObject();

                // MonoKey key
                JsonObject monoKeyKeyObj = monoKeyObj.getAsJsonObject("keyCode");

                // MonoKey key name
                String monoKeyKeyName;
                if (monoKeyKeyObj.has("field_1663")) monoKeyKeyName = monoKeyKeyObj.get("field_1663").getAsString();
                else if (monoKeyKeyObj.has("f_84853_")) monoKeyKeyName = monoKeyKeyObj.get("f_84853_").getAsString();
                else monoKeyKeyName = monoKeyKeyObj.get("name").getAsString();

                // MonoKey messages
                ArrayList<String> mkoMessages = new ArrayList<>(List.of(
                        monoKeyObj.get("msg").getAsString().split(",,")));

                monoKeySet.add(new CommandMonoKey(new TriState(), new QuadState(), true,
                        false, 0, InputConstants.getKey(monoKeyKeyName), mkoMessages));

            }


            LegacyCodeMsgMap legacyCodeMsgMap = new GsonBuilder().create().fromJson(json,
                    LegacyCodeMsgMap.class);
            for (int keyCode : legacyCodeMsgMap.codeMsgMapDual.keySet()) {
                ArrayList<String> messages = new ArrayList<>(List.of(
                        legacyCodeMsgMap.codeMsgMapDual.get(keyCode).split(",,")));
                dualKeySet.add(new CommandDualKey(InputConstants.getKey(keyCode, keyCode), messages));
            }

            return new Config(addToHistory, showHudMessage, monoKeySet,
                    addToHistory, showHudMessage, dualKeySet);
        }
    }

    public class LegacyCodeMsgMap {
        Map<Integer,String> codeMsgMapDual;
    }
}
