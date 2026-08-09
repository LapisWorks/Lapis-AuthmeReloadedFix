package mc506lw.lapisAuthmeReloadedFix;

import mc506lw.lapisAuthmeReloadedFix.bridge.AuthmeBridge;
import mc506lw.lapisAuthmeReloadedFix.bridge.FloodgateBridge;
import mc506lw.lapisAuthmeReloadedFix.bridge.FppBridge;
import mc506lw.lapisAuthmeReloadedFix.config.FixConfig;
import mc506lw.lapisAuthmeReloadedFix.integration.AuthmePlaceholderExpansion;
import mc506lw.lapisAuthmeReloadedFix.listener.BedrockAutoLoginListener;
import mc506lw.lapisAuthmeReloadedFix.listener.FppBotAutoLoginListener;
import mc506lw.lapisAuthmeReloadedFix.listener.GhostPlayerListener;
import mc506lw.lapisAuthmeReloadedFix.util.BotAccountStore;
import mc506lw.lapisAuthmeReloadedFix.util.Scheduling;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LapisAuthmeReloadedFix extends JavaPlugin {

    private FixConfig config;
    private AuthmeBridge authmeBridge;
    private FloodgateBridge floodgateBridge;
    private FppBridge fppBridge;
    private BotAccountStore botAccountStore;
    private FppBotAutoLoginListener fppBotListener;
    private AuthmePlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        config = new FixConfig(this);
        config.load();

        authmeBridge = new AuthmeBridge(this);
        floodgateBridge = new FloodgateBridge();
        fppBridge = new FppBridge();
        botAccountStore = new BotAccountStore(this);
        botAccountStore.load();

        getServer().getPluginManager().registerEvents(new GhostPlayerListener(config, fppBridge), this);
        getServer().getPluginManager().registerEvents(
            new BedrockAutoLoginListener(this, config, authmeBridge, floodgateBridge), this);
        fppBotListener = new FppBotAutoLoginListener(this, config, authmeBridge, fppBridge, botAccountStore);
        getServer().getPluginManager().registerEvents(fppBotListener, this);

        registerPlaceholderExpansion();

        if (config.isFppBotAutoLoginEnabled() && config.isFppCleanupOnStartup()
                && authmeBridge.isAvailable() && !botAccountStore.snapshot().isEmpty()) {
            Scheduling.runLaterGlobal(this, fppBotListener::cleanupStaleAccounts, 40L);
        }

        getLogger().info("Lapis-AuthmeReloadedFix enabled. Folia: " + Scheduling.isFolia()
            + " | AuthMe: " + authmeBridge.isAvailable()
            + " | Floodgate: " + floodgateBridge.isAvailable()
            + " | FPP: " + fppBridge.isAvailable());
    }

    @Override
    public void onDisable() {
        if (fppBotListener != null) {
            try {
                for (Player player : getServer().getOnlinePlayers()) {
                    if (fppBridge.isBot(player)) {
                        fppBotListener.removeSessionAccount(player.getName());
                    }
                }
                fppBotListener.getAccountStore().persist();
            } catch (Throwable t) {
                getLogger().warning("Failed to clean FPP bot accounts on shutdown: " + t.getMessage());
            }
        }
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
    }

    private void registerPlaceholderExpansion() {
        if (!config.isPlaceholderApiEnabled() || getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }
        placeholderExpansion = new AuthmePlaceholderExpansion(authmeBridge);
        placeholderExpansion.register();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("lapisauthmefix")) {
            return false;
        }
        if (args.length == 1 && "reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("lapisauthmefix.reload")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to reload the config.");
                return true;
            }
            config.reload();
            registerPlaceholderExpansion();
            sender.sendMessage(ChatColor.GREEN + "Lapis-AuthmeReloadedFix config reloaded.");
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " reload");
        return true;
    }
}