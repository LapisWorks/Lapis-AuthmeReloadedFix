package mc506lw.lapisAuthmeReloadedFix.bridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Reflection-based bridge to the FakePlayerPlugin (FPP) API. Detects fake players so AuthMe criminalize
 * auto-login can skip/auto-login bots without hard-compiling against the FPP jar.
 */
public final class FppBridge {

    private Method getApiMethod;
    private Method isBotMethod;

    private Object resolvedApi() {
        Plugin fpp = Bukkit.getPluginManager().getPlugin("FakePlayerPlugin");
        if (fpp == null) {
            return null;
        }
        try {
            if (getApiMethod == null) {
                getApiMethod = fpp.getClass().getMethod("getFppApi");
            }
            Object api = getApiMethod.invoke(fpp);
            if (api != null && isBotMethod == null) {
                isBotMethod = api.getClass().getMethod("isBot", Player.class);
            }
            return api;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    public boolean isAvailable() {
        return resolvedApi() != null;
    }

    public boolean isBot(Player player) {
        Object api = resolvedApi();
        if (api == null || player == null || isBotMethod == null) {
            return false;
        }
        try {
            return (Boolean) isBotMethod.invoke(api, player);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }
}