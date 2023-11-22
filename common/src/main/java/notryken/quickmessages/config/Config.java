package notryken.quickmessages.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
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
            .setPrettyPrinting().create();

    // Not saved, not user-accessible
    private static Path configPath;
    private static Map<KeyMapping,String> keyMsgMapMono;

    // Saved, not user-accessible
    private final String version = "001";

    // Saved, user-accessible
    public boolean showHudMessage;
    public boolean addToHistory;
    private final Map<Integer,String> codeMsgMapDual;
    private Map<Integer,String> codeMsgMapMono;


    public Config() {
        showHudMessage = true;
        addToHistory = true;
        codeMsgMapDual = new LinkedHashMap<>();
        codeMsgMapMono = new LinkedHashMap<>();
        keyMsgMapMono = new LinkedHashMap<>();
    }

    // Config load and save

    public static Config load() {
        return load(DEFAULT_FILE_NAME);
    }

    public static Config load(String name) {
        Path path = Path.of("config").resolve(name);
        Config config;

        if (Files.exists(path)) {
            try (FileReader reader = new FileReader(path.toFile())) {
                configPath = path;
                config = GSON.fromJson(reader, Config.class);
                /* TODO need either validation or deserializer, since old config
                    only has "messageMap".
                 */
                config.loadMonoMap();
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
        if (configPath != null) {
            Path dir = configPath.getParent();

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

    public String getMsgMono(KeyMapping key) {
        return keyMsgMapMono.get(key);
    }

    public Iterator<KeyMapping> getKeyIterMono() {
        return keyMsgMapMono.keySet().iterator();
    }

    public Iterator<String> getValIterMono() {
        return keyMsgMapMono.values().iterator();
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
            codeMsgMapDual.put(Integer.MAX_VALUE, "");
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

    public boolean setKey2(KeyMapping oldKey, KeyMapping newKey) {
        String message = keyMsgMapMono.get(oldKey);
        if (message != null && !keyMsgMapMono.containsKey(newKey)) {
            keyMsgMapMono.remove(oldKey);
            keyMsgMapMono.put(newKey, message);
            return true;
        }
        return false;
    }

    public void setMsgMono(KeyMapping key, String newMessage) {
        keyMsgMapMono.replace(key, newMessage);
    }

    public boolean addMsgMono() {
        KeyMapping NEW_KEY = new KeyMapping(
                "key.quickmessages.new_key", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, "keygroup.quickmessages.title");

        if (!keyMsgMapMono.containsKey(NEW_KEY)) {
            keyMsgMapMono.put(NEW_KEY, "");
            return true;
        }
        return false;
    }

    public boolean removeMsgMono(KeyMapping key) {
        return keyMsgMapMono.remove(key) != null;
    }

    public void purge() {
        codeMsgMapDual.values().removeIf(String::isBlank);
        codeMsgMapMono.values().removeIf(String::isBlank);
        keyMsgMapMono.values().removeIf(String::isBlank);
    }

    public void loadMonoMap() {
        keyMsgMapMono = new LinkedHashMap<>();
        Iterator<Integer> keyIter = codeMsgMapMono.keySet().iterator();
        Iterator<String> valIter = codeMsgMapMono.values().iterator();
        while (keyIter.hasNext()) {
            int key = keyIter.next();
            keyMsgMapMono.put(new KeyMapping(String.valueOf(key), InputConstants.Type.KEYSYM,
                    key, "keygroup.quickmessages.title"), valIter.next());
        }
    }

    public void syncMonoMap() {
        codeMsgMapMono = new LinkedHashMap<>();
        Iterator<KeyMapping> keyIter = keyMsgMapMono.keySet().iterator();
        Iterator<String> valIter = keyMsgMapMono.values().iterator();
        while (keyIter.hasNext()) {
            codeMsgMapMono.put(keyIter.next().key.getValue(), valIter.next());
        }
    }
}
