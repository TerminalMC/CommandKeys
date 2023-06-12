package notryken.quickmessages.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import notryken.quickmessages.config.Config;
import notryken.quickmessages.config.ConfigDeserializer;
import notryken.quickmessages.gui.ConfigScreen;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class QuickMessagesClient implements ClientModInitializer
{
    public static Config config;

    private static final File settingsFile =
            new File("config", "quickmessages.json");
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Config.class, new ConfigDeserializer())
            .setPrettyPrinting().create();
    private static KeyBinding keyBinding;

    @Override
    public void onInitializeClient()
    {
        keyBinding = new KeyBinding("Open Menu",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "Quick Messages");
        KeyBindingHelper.registerKeyBinding(keyBinding);

        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);

        loadConfig();
    }

    private void onEndTick(MinecraftClient client)
    {
        while (keyBinding.wasPressed()) {
            MinecraftClient.getInstance().setScreen(new ConfigScreen(
                    client.currentScreen));
        }
    }

    /**
     * If the config file exists and is readable, loads the config from it,
     * correcting any invalid fields. If it exists but is unreadable, creates a
     * new config with defaults. If it does not exist, creates it with defaults.
     * Finally, saves the validated config.
     */
    public static void loadConfig()
    {
        if (settingsFile.exists()) {
            try {
                config = gson.fromJson(Files.readString(settingsFile.toPath()),
                        Config.class);
            } catch (Exception e) { // Catching Exception to cover all bases.
                System.err.println(e.getMessage());
                config = null;
            }
        }
        if (config == null) {
            config = new Config();
        }
        saveConfig();
    }

    /**
     * Writes the current config to the config file, overwriting any existing
     * value.
     */
    public static void saveConfig()
    {
        try {
            Files.writeString(settingsFile.toPath(), gson.toJson(config));
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
        }
    }
}
