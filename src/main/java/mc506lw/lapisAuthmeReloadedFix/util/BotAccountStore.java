package mc506lw.lapisAuthmeReloadedFix.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Persists the names of AuthMe accounts that this plugin created for FPP bots, so that they can be
 * cleaned up even if the server crashed or shut down while a bot was still "logged in".
 */
public final class BotAccountStore {

    private final File storeFile;
    private final Set<String> names = new HashSet<>();
    private volatile boolean dirty;

    public BotAccountStore(JavaPlugin plugin) {
        this.storeFile = new File(plugin.getDataFolder(), "bot-accounts.yml");
    }

    public void load() {
        names.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(storeFile);
        for (Object value : config.getList("names", java.util.Collections.emptyList())) {
            if (value instanceof String) {
                names.add(((String) value).toLowerCase(Locale.ROOT));
            }
        }
    }

    public boolean contains(String playerName) {
        return names.contains(playerName.toLowerCase(Locale.ROOT));
    }

    public void add(String playerName) {
        names.add(playerName.toLowerCase(Locale.ROOT));
        dirty = true;
    }

    public boolean remove(String playerName) {
        if (names.remove(playerName.toLowerCase(Locale.ROOT))) {
            dirty = true;
            return true;
        }
        return false;
    }

    public Set<String> snapshot() {
        return new HashSet<>(names);
    }

    /** Writes the store file if it changed. Called periodically and on disable. */
    public void persistIfNeeded() {
        if (!dirty) {
            return;
        }
        persist();
    }

    public void persist() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("names", new java.util.ArrayList<>(names));
        try {
            File parent = storeFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return;
            }
            config.save(storeFile);
            dirty = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}