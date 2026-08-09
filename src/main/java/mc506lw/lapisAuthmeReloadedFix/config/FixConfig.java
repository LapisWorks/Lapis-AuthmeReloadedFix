package mc506lw.lapisAuthmeReloadedFix.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class FixConfig {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public FixConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public boolean isGhostPlayerFixEnabled() {
        return config.getBoolean("ghost-player-fix.enabled", true);
    }

    public String getGhostPlayerKickMessage() {
        return config.getString("ghost-player-fix.kick-message",
            "&cA player with the same name is already online. Please try again.");
    }

    public boolean isBedrockAutoLoginEnabled() {
        return config.getBoolean("bedrock-auto-login.enabled", true);
    }

    public boolean isBedrockOnlyRegistered() {
        return config.getBoolean("bedrock-auto-login.only-registered", true);
    }

    public boolean isFppBotAutoLoginEnabled() {
        return config.getBoolean("fpp-bot-auto-login.enabled", true);
    }

    public boolean isFppCleanupOnStartup() {
        return config.getBoolean("fpp-bot-auto-login.cleanup-on-startup", true);
    }

    public boolean isPlaceholderApiEnabled() {
        return config.getBoolean("placeholder-api.enabled", true);
    }
}
