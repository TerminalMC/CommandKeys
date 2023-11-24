package notryken.quickmessages.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import notryken.quickmessages.Constants;
import notryken.quickmessages.config.deserialize.InputConstantsKeyDeserializer;
import notryken.quickmessages.config.deserialize.KeyMappingDeserializer;
import notryken.quickmessages.config.legacy.LegacyConfig;
import org.lwjgl.glfw.GLFW;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Config {
    // Constants
    public static final String DEFAULT_FILE_NAME = "quickmessages.json";
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(InputConstants.Key.class, new InputConstantsKeyDeserializer())
            .registerTypeAdapter(KeyMapping.class, new KeyMappingDeserializer())
            .setPrettyPrinting().create();
    private static final Gson LEGACY_GSON = new GsonBuilder()
            .setPrettyPrinting().create();

    // Not saved, not user-accessible
    private static Path configPath;

    // Saved, not user-accessible
    private final String version = "001";

    // Saved, user-accessible
    public boolean showHudMessage;
    public boolean addToHistory;
    private final Map<Integer,String> codeMsgMapDual;
    private final Set<MsgKeyMapping> msgKeyListMono;


    public Config() {
        showHudMessage = true;
        addToHistory = true;
        codeMsgMapDual = new LinkedHashMap<>();
        msgKeyListMono = new LinkedHashSet<>();
    }

    public Config(Map<Integer,String> codeMsgMapDual) {
        showHudMessage = true;
        addToHistory = true;
        this.codeMsgMapDual = codeMsgMapDual;
        msgKeyListMono = new LinkedHashSet<>();
    }

    // Config load and save

    public static Config load() {
        return load(DEFAULT_FILE_NAME);
    }

    public static Config load(String name) {
        configPath = Path.of("config").resolve(name);
        Config config;

        if (Files.exists(configPath)) {
            try (FileReader reader = new FileReader(configPath.toFile())) {
                /*
                Check the config file to determine version. v1.0.1 has
                "messageMap" as the first stored identifier. v1.1.0 and higher
                have "version". v1.0.0 and lower are unsupported from v1.0.1.

                Backwards-compatibility to v1.0.1 to be maintained until it can
                be reasonably surmised that the vast majority of v1.0.1 users
                have updated to v1.1.0 or higher.

                Uses a second reader because apparently FileReader.reset()
                isn't allowed.
                 */
                try (FileReader checkReader = new FileReader(configPath.toFile())) {
                    for (int i = 0; i < 5; i++) {
                        checkReader.read();
                    }
                    if (checkReader.read() == 118) {
                        config = GSON.fromJson(reader, Config.class);
                    }
                    else {
                        Constants.LOG.info("Config file using legacy format, applying legacy deserializer.");
                        LegacyConfig legacyConfig = LEGACY_GSON.fromJson(reader, LegacyConfig.class);
                        config = new Config(legacyConfig.messageMap);
                    }

                }
            } catch (IOException e) {
                throw new RuntimeException("Could not parse config", e);
            }
        } else {
            config = new Config();
        }

        config.writeChanges();
        return config;
    }

    public void writeChanges() {
        Path dir = configPath == null ? Path.of("config") : configPath.getParent();

        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            } else if (!Files.isDirectory(dir)) {
                throw new IOException("Not a directory: " + dir);
            }

            // Use a temporary location next to the config's final destination
            Path tempPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");

            // Write the file to our temporary location
            Files.writeString(tempPath, GSON.toJson(this));

            // Atomically replace the old config file (if it exists) with the temporary file
            Files.move(tempPath, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't update config file", e);
        }

    }

    // Accessors

    public String getMsgDual(int code) {
        return codeMsgMapDual.get(code);
    }

    public Iterator<Integer> getKeyIterDual() {
        return codeMsgMapDual.keySet().iterator();
    }

    public Iterator<String> getValIterDual() {
        return codeMsgMapDual.values().iterator();
    }

    public List<MsgKeyMapping> getMsgKeyListMono() {
        return msgKeyListMono.stream().toList();
    }

    // Dual-key map mutators

    /**
     * If there is an entry with specified keyCode, changes its keyCode to
     * specified newKeyCode.
     * @param keyCode the original keycode.
     * @param newKeyCode the new keycode.
     */
    public boolean setKeyDual(int keyCode, int newKeyCode) {
        String message = codeMsgMapDual.get(keyCode);
        if (message != null && !codeMsgMapDual.containsKey(newKeyCode)) {
            codeMsgMapDual.remove(keyCode);
            codeMsgMapDual.put(newKeyCode, message);
            return true;
        }
        return false;
    }

    /**
     * If there is an entry with specified keyCode, changes its message to
     * specified newMessage.
     * @param keyCode the message keyCode.
     * @param newMessage the new content.
     */
    public void setMsgDual(int keyCode, String newMessage) {
        codeMsgMapDual.replace(keyCode, newMessage);
    }

    /**
     * Adds a new entry with keyCode GLFW.GLFW_KEY_UNKNOWN and message "", if
     * one does not already exist.
     */
    public boolean addMsgDual() {
        if (!codeMsgMapDual.containsKey(GLFW.GLFW_KEY_UNKNOWN)) {
            codeMsgMapDual.put(GLFW.GLFW_KEY_UNKNOWN, "");
            return true;
        }
        return false;
    }

    /**
     * Removes the entry with specified keyCode, if it exists.
     * @param keyCode the input keycode of the entry.
     */
    public boolean removeMsgDual(int keyCode) {
        return codeMsgMapDual.remove(keyCode) != null;
    }

    // Mono-key map mutators

    public boolean addMsgKeyMono() {
        MsgKeyMapping msgKey = new MsgKeyMapping();
        if (!msgKeyListMono.contains(msgKey)) {
            msgKeyListMono.add(msgKey);
            return true;
        }
        return false;
    }

    public boolean removeMsgKeyMono(MsgKeyMapping msgKey) {
        return msgKeyListMono.remove(msgKey);
    }

    public void purge() {
        codeMsgMapDual.values().removeIf(String::isBlank);
        msgKeyListMono.removeIf((MsgKeyMapping) -> MsgKeyMapping.msg.isBlank());
    }

    public void checkDuplicatesMono() {
        for (MsgKeyMapping msgKey : msgKeyListMono) {
            msgKey.checkDuplicated(msgKey.keyCode);
        }
    }
}
