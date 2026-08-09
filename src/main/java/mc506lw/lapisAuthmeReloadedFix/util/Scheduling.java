package mc506lw.lapisAuthmeReloadedFix.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Scheduler helper that transparently works on both Paper and Folia.
 * On Folia, tasks that touch a player must be scheduled on that player's region thread.
 */
public final class Scheduling {

    private static final boolean FOLIA = isFolia();

    private Scheduling() {
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }

    /**
     * Runs a task on the main/region thread a few ticks later.
     * The task is guaranteed to be executed on the correct thread for the given entity.
     *
     * @param plugin the fix plugin
     * @param player the player the task is associated with (used for region selection on Folia)
     * @param task   the task to run
     * @param delayTicks delay in ticks
     */
    public static void runLater(JavaPlugin plugin, Player player, Runnable task, long delayTicks) {
        if (FOLIA) {
            Bukkit.getRegionScheduler().runDelayed(plugin, player.getLocation(), t -> task.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Runs a task a few ticks later without a specific region/player association.
     */
    public static void runLaterGlobal(JavaPlugin plugin, Runnable task, long delayTicks) {
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }
}