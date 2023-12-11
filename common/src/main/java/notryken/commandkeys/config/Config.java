package notryken.commandkeys.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import notryken.commandkeys.config.deserialize.InputConstantsKeyDeserializer;
import notryken.commandkeys.config.deserialize.KeyMappingDeserializer;
import notryken.commandkeys.config.legacy.LegacyConfig;
import org.lwjgl.glfw.GLFW;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Config {
    // Constants
    public static final String DEFAULT_FILE_NAME = "commandkeys_v1.json";
    public static final String LEGACY_FILE_NAME = "quickmessages.json";
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(InputConstants.Key.class, new InputConstantsKeyDeserializer())
            .registerTypeAdapter(KeyMapping.class, new KeyMappingDeserializer())
            .setPrettyPrinting().create();
    private static final Gson LEGACY_GSON = new GsonBuilder()
            .setPrettyPrinting().create();

    // Not saved, not user-accessible
    private static Path configPath;
    public static boolean configChecked;

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
        return load(DEFAULT_FILE_NAME, LEGACY_FILE_NAME);
    }

    public static Config load(String name, String backup) {
        Path configDir = Path.of("config");
        configPath = configDir.resolve(name);
        Path backupPath = configDir.resolve(backup);
        Config config;

        if (Files.exists(configPath)) {
            try (FileReader reader = new FileReader(configPath.toFile())) {
                config = GSON.fromJson(reader, Config.class);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse config", e);
            }
        }
        else if (Files.exists(backupPath)) {
            try (FileReader reader = new FileReader(backupPath.toFile())) {
                LegacyConfig legacyConfig = LEGACY_GSON.fromJson(reader, LegacyConfig.class);
                config = new Config(legacyConfig.messageMap);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse config", e);
            }
        }
        else {
            config = new Config();
        }

        config.purge();
        /*
        Required in case Minecraft instance is initialized before
        CommandKeys config.
         */
        try {
            KeyMapping[] temp = Minecraft.getInstance().options.keyMappings;
            config.checkDuplicatesMono();
        }
        catch (NullPointerException e) {
            // Pass
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
    public void setKeyDual(int keyCode, int newKeyCode) {
        // Inefficient workaround to maintain order
        if (codeMsgMapDual.get(keyCode) != null && !codeMsgMapDual.containsKey(newKeyCode)) {
            Map<Integer,String> newMap = new LinkedHashMap<>();
            Iterator<Integer> keyIter = getKeyIterDual();
            Iterator<String> valIter = getValIterDual();
            while (keyIter.hasNext()) {
                int key = keyIter.next();
                if (key == keyCode) {
                    newMap.put(newKeyCode, valIter.next());
                }
                else {
                    newMap.put(key, valIter.next());
                }
            }
            codeMsgMapDual.clear();
            codeMsgMapDual.putAll(newMap);
        }
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
        configChecked = true;
    }
}
