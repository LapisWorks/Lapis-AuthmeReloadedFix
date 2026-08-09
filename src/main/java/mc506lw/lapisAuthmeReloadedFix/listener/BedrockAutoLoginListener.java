package mc506lw.lapisAuthmeReloadedFix.listener;

import mc506lw.lapisAuthmeReloadedFix.bridge.AuthmeBridge;
import mc506lw.lapisAuthmeReloadedFix.bridge.FloodgateBridge;
import mc506lw.lapisAuthmeReloadedFix.config.FixConfig;
import mc506lw.lapisAuthmeReloadedFix.util.Scheduling;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Auto-login for bedrock (Geyser/Floodgate) players, restoring the AuthMeReReloaded
 * "BedrockAutoLoginListener" behaviour that was removed in AuthMe 6.
 */
public final class BedrockAutoLoginListener implements Listener {

    private final JavaPlugin plugin;
    private final FixConfig config;
    private final AuthmeBridge authmeBridge;
    private final FloodgateBridge floodgateBridge;

    public BedrockAutoLoginListener(JavaPlugin plugin, FixConfig config,
                                    AuthmeBridge authmeBridge, FloodgateBridge floodgateBridge) {
        this.plugin = plugin;
        this.config = config;
        this.authmeBridge = authmeBridge;
        this.floodgateBridge = floodgateBridge;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!config.isBedrockAutoLoginEnabled() || !floodgateBridge.isAvailable() || !authmeBridge.isAvailable()) {
            return;
        }
        Player player = event.getPlayer();
        if (!floodgateBridge.isFloodgatePlayer(player) || authmeBridge.isAuthenticated(player)) {
            return;
        }
        Scheduling.runLater(plugin, player, () -> {
            if (player.isOnline() && !authmeBridge.isAuthenticated(player)) {
                if (!config.isBedrockOnlyRegistered() || authmeBridge.isRegistered(player.getName())) {
                    authmeBridge.forceLogin(player);
                }
            }
        }, 1L);
    }
}