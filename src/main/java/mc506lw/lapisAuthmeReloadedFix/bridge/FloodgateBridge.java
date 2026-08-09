package mc506lw.lapisAuthmeReloadedFix.bridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Reflection-based bridge to the Floodgate (bedrock/Geyser) API.
 */
public final class FloodgateBridge {

    private Class<?> apiClass;
    private Method getInstanceMethod;
    private Method isFloodgateMethod;

    private boolean resolve() {
        if (apiClass != null) {
            return true;
        }
        if (Bukkit.getPluginManager().getPlugin("floodgate") == null) {
            return false;
        }
        try {
            Class<?> c = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            getInstanceMethod = c.getMethod("getInstance");
            // Prefer the shape (UUID) style used in older versions, fall back to (Player).
            try {
                isFloodgateMethod = c.getMethod("isFloodgateId", UUID.class);
            } catch (NoSuchMethodException e) {
                try {
                    isFloodgateMethod = c.getMethod("isFloodgatePlayer", UUID.class);
                } catch (NoSuchMethodException e2) {
                    isFloodgateMethod = c.getMethod("isFromGeyser", Player.class);
                }
            }
            apiClass = c;
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError e) {
            return false;
        }
    }

    public boolean isAvailable() {
        return resolve();
    }

    public boolean isFloodgatePlayer(Player player) {
        if (player == null || !resolve()) {
            return false;
        }
        try {
            Object api = getInstanceMethod.invoke(null);
            if (api == null) {
                return false;
            }
            if (isFloodgateMethod.getParameterTypes()[0] == Player.class) {
                return (Boolean) isFloodgateMethod.invoke(api, player);
            }
            return (Boolean) isFloodgateMethod.invoke(api, player.getUniqueId());
        } catch (IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }
}