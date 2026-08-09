package mc506lw.lapisAuthmeReloadedFix.listener;

import mc506lw.lapisAuthmeReloadedFix.bridge.AuthmeBridge;
import mc506lw.lapisAuthmeReloadedFix.bridge.FppBridge;
import mc506lw.lapisAuthmeReloadedFix.config.FixConfig;
import mc506lw.lapisAuthmeReloadedFix.util.BotAccountStore;
import mc506lw.lapisAuthmeReloadedFix.util.Scheduling;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Auto-login for FakePlayerPlugin (FPP) bots against AuthMe 6.
 * <p>
 * AuthMe 6's {@code forceLogin} is a no-op for players without a database account, so bots get kicked by
 * the login/registration timeout. This listener creates a short-lived account for the bot instead:
 * <ul>
 *   <li>unknown name {@literal ->} force-register with a random password (auto-login), remember it;</li>
 *   <li>name already registered AND in our store {@literal ->} leftover bot account from a previous run, force-login;</li>
 *   <li>name already registered but NOT ours {@literal ->} likely a real player's account: we skip it so we
 *       never "log into" someone else's account;</li>
 * </ul>
 * Accounts are removed when the bot leaves (and on shutdown by the main class). The store file survives
 * crash/restart so stale bot accounts can be purged on the next startup with {@link #cleanupStaleAccounts()}.
 */
public final class FppBotAutoLoginListener implements Listener {

    private final JavaPlugin plugin;
    private final FixConfig config;
    private final AuthmeBridge authmeBridge;
    private final FppBridge fppBridge;
    private final BotAccountStore accountStore;
    private final SecureRandom random = new SecureRandom();

    public FppBotAutoLoginListener(JavaPlugin plugin, FixConfig config, AuthmeBridge authmeBridge,
                                   FppBridge fppBridge, BotAccountStore accountStore) {
        this.plugin = plugin;
        this.config = config;
        this.authmeBridge = authmeBridge;
        this.fppBridge = fppBridge;
        this.accountStore = accountStore;
    }

    public BotAccountStore getAccountStore() {
        return accountStore;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!config.isFppBotAutoLoginEnabled() || !fppBridge.isAvailable() || !authmeBridge.isAvailable()) {
            return;
        }
        Player player = event.getPlayer();
        if (!fppBridge.isBot(player) || authmeBridge.isAuthenticated(player)) {
            return;
        }
        Scheduling.runLater(plugin, player, () -> {
            if (!player.isOnline() || !fppBridge.isBot(player) || authmeBridge.isAuthenticated(player)) {
                return;
            }
            registerOrLoginBot(player);
        }, 1L);
    }

    private void registerOrLoginBot(Player player) {
        String name = player.getName();
        String key = name.toLowerCase(Locale.ROOT);
        if (authmeBridge.isRegistered(key)) {
            if (accountStore.contains(key)) {
                authmeBridge.forceLogin(player);
                plugin.getLogger().info("FPP bot '" + name + "' matched our stored bot account - login.");
            } else {
                plugin.getLogger().warning("FPP bot '" + name + "' matches an existing account not created by this "
                    + "plugin (probably a real player). Skipped auto-login to avoid hijacking it.");
            }
        } else {
            accountStore.add(key);
            authmeBridge.forceRegister(player, randomPassword(), true);
            plugin.getLogger().info("FPP bot '" + name + "' auto-registered + logged in (temp account recorded).");
        }
        accountStore.persistIfNeeded();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!config.isFppBotAutoLoginEnabled() || !fppBridge.isAvailable() || !authmeBridge.isAvailable()) {
            return;
        }
        Player player = event.getPlayer();
        if (!fppBridge.isBot(player)) {
            return;
        }
        removeSessionAccount(player.getName());
    }

    /**
     * Unregisters bot accounts we created for the given player (e.g. on quit or graceful shutdown).
     */
    public void removeSessionAccount(String playerName) {
        String key = playerName.toLowerCase(Locale.ROOT);
        if (accountStore.remove(key)) {
            authmeBridge.forceUnregister(playerName);
            plugin.getLogger().info("FPP bot '" + playerName + "' left - temporary account removed.");
        }
        accountStore.persistIfNeeded();
    }

    /**
     * Purges stale bot accounts left behind after a crash/restart (accounts recorded in our store that
     * AuthMe still holds). Only ever touches names written by this plugin, never real player accounts.
     */
    public void cleanupStaleAccounts() {
        if (!authmeBridge.isAvailable()) {
            return;
        }
        int removed = 0;
        // Safe-guard: do not purge names of currently online players
        java.util.Set<String> online = new java.util.HashSet<>();
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            online.add(p.getName().toLowerCase(Locale.ROOT));
        }
        for (String key : accountStore.snapshot()) {
            if (online.contains(key)) {
                continue;
            }
            if (authmeBridge.isRegistered(key)) {
                authmeBridge.forceUnregister(key);
            }
            accountStore.remove(key);
            removed++;
        }
        accountStore.persistIfNeeded();
        if (removed > 0) {
            plugin.getLogger().info("Cleaned up " + removed + " stale FPP bot account(s) from AuthMe database.");
        }
    }

    private String randomPassword() {
        return "Lapisix-" + Long.toUnsignedString(random.nextLong() & Long.MAX_VALUE, 36);
    }
}