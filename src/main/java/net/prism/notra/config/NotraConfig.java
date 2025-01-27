package net.prism.notra.config;

import com.moandjiezana.toml.Toml;
import net.fabricmc.loader.api.FabricLoader;
import net.prism.notra.Notra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class NotraConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("notra.toml");
    private static NotraConfig INSTANCE;
    public boolean shopNotifications;
    public boolean shopNotificationSound;
    public boolean shopChatMessages;
    public Map<String, Boolean> shopSettings = new HashMap<>();

    private NotraConfig() {
        load();
    }

    public static NotraConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NotraConfig();
        }
        return INSTANCE;
    }

    public void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                createDefaultConfig();
            }
            Toml toml = new Toml().read(CONFIG_PATH.toFile());
            shopNotifications = toml.getBoolean("notifications.ShopNotifications", true);
            shopNotificationSound = toml.getBoolean("notifications.ShopNotificationSound", true);
            shopChatMessages = toml.getBoolean("notifications.ShopChatMessages", true);
            Toml shopsSection = toml.getTable("shops");
            String[] defaults = {"Buckstar","Bakery","Cocktail","Cheese"};
            if (shopsSection == null) {
                for (String s : defaults) {
                    shopSettings.put(s, true);
                }
            } else {
                for (String s : defaults) {
                    shopSettings.put(s, shopsSection.getBoolean(s, true));
                }
            }
        } catch (Exception e) {
            Notra.LOGGER.error("Failed to load Notra config", e);
        }
    }

    private void createDefaultConfig() throws IOException {
        String defaultConfig = """
            [notifications]
            ShopNotifications = true
            ShopNotificationSound = true
            ShopChatMessages = true

            [shops]
            Buckstar = true
            Bakery = true
            Cocktail = true
            Cheese = true
            """;
        Files.writeString(CONFIG_PATH, defaultConfig);
    }
}
