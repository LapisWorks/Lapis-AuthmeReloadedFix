package mc506lw.lapisAuthmeReloadedFix.listener;

import mc506lw.lapisAuthmeReloadedFix.bridge.FppBridge;
import mc506lw.lapisAuthmeReloadedFix.config.FixConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Locale;

/**
 * Mirrors the AuthMeReReloaded "DoubleLoginFixListener" / anti-ghost-player behaviour that was
 * dropped in AuthMe 6: if another player with the same name is already online, kick the stale one.
 * FakePlayerPlugin bots are never kicked.
 */
public final class GhostPlayerListener implements Listener {

    private final FixConfig config;
    private final FppBridge fppBridge;

    public GhostPlayerListener(FixConfig config, FppBridge fppBridge) {
        this.config = config;
        this.fppBridge = fppBridge;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!config.isGhostPlayerFixEnabled()) {
            return;
        }
        Player joined = event.getPlayer();
        if (fppBridge.isBot(joined)) {
            return;
        }
        String key = joined.getName().toLowerCase(Locale.ROOT);
        Player ghost = null;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == joined) {
                continue;
            }
            if (online.getName().toLowerCase(Locale.ROOT).equals(key)) {
                ghost = online;
                break;
            }
        }
        if (ghost != null && !fppBridge.isBot(ghost)) {
            String message = ChatColor.translateAlternateColorCodes('&', config.getGhostPlayerKickMessage());
            ghost.kickPlayer(message);
        }
    }
}