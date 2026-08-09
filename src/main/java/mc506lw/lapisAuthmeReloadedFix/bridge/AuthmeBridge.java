package mc506lw.lapisAuthmeReloadedFix.bridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Reflection-based bridge to the AuthMe (AuthmeApi / v3) API so that this plugin does not need to
 * compile against the AuthMe jar and stays compatible with both the 5.x and 6.x code states.
 */
public final class AuthmeBridge {

    private final JavaPlugin plugin;

    private Class<?> apiClass;
    private Method getInstanceMethod;
    private Method isAuthenticatedMethod;
    private Method isRegisteredMethod;
    private Method forceLoginMethod;
    private Method forceRegisterMethod;
    private Method forceUnregisterPlayerMethod;
    private Method forceUnregisterNameMethod;
    private Method getPlayerInfoMethod;

    public AuthmeBridge(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... params) {
        try {
            return clazz.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private synchronized boolean resolve() {
        if (apiClass != null) {
            return true;
        }
        if (Bukkit.getPluginManager().getPlugin("AuthMe") == null) {
            return false;
        }
        for (String candidate : new String[]{"fr.xephi.authme.api.v3.AuthmeApi",
            "fr.xephi.authme.api.v3.AuthMeApi"}) {
            try {
                Class<?> c = Class.forName(candidate);
                getInstanceMethod = findMethod(c, "getInstance");
                isAuthenticatedMethod = findMethod(c, "isAuthenticated", Player.class);
                isRegisteredMethod = findMethod(c, "isRegistered", String.class);
                forceLoginMethod = findMethod(c, "forceLogin", Player.class);
                forceRegisterMethod = findMethod(c, "forceRegister", Player.class, String.class, boolean.class);
                forceUnregisterPlayerMethod = findMethod(c, "forceUnregister", Player.class);
                forceUnregisterNameMethod = findMethod(c, "forceUnregister", String.class);
                getPlayerInfoMethod = findMethod(c, "getPlayerInfo", String.class);
                if (getInstanceMethod != null && isRegisteredMethod != null && forceLoginMethod != null) {
                    apiClass = c;
                    plugin.getLogger().info("AuthMe API bound: " + candidate
                        + " (getPlayerInfo=" + (getPlayerInfoMethod != null) + ")");
                    return true;
                }
            } catch (ClassNotFoundException | LinkageError e) {
                // try next candidate
            }
        }
        return false;
    }

    private Object apiInstance() {
        if (!resolve()) {
            return null;
        }
        try {
            return getInstanceMethod.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    public boolean isAvailable() {
        return apiInstance() != null;
    }

    /** Short status string for the {@code %authme_debug%} placeholder / diagnostics. */
    public String describeBinding() {
        return "api=" + (apiInstance() != null)
            + ",getPlayerInfo=" + (getPlayerInfoMethod != null)
            + ",forceRegister=" + (forceRegisterMethod != null)
            + ",forceUnregister=" + (forceUnregisterNameMethod != null || forceUnregisterPlayerMethod != null);
    }

    public boolean isAuthenticated(Player player) {
        Object api = apiInstance();
        if (api == null || isAuthenticatedMethod == null) {
            return false;
        }
        try {
            return (Boolean) isAuthenticatedMethod.invoke(api, player);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }

    public boolean isRegistered(String playerName) {
        Object api = apiInstance();
        if (api == null || isRegisteredMethod == null) {
            return false;
        }
        try {
            return (Boolean) isRegisteredMethod.invoke(api, playerName);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }

    public void forceLogin(Player player) {
        Object api = apiInstance();
        if (api == null || forceLoginMethod == null || player == null) {
            return;
        }
        try {
            forceLoginMethod.invoke(api, player);
        } catch (IllegalAccessException | InvocationTargetException e) {
            plugin.getLogger().warning("Failed to force authme login for '" + player.getName() + "': " + e.getCause());
        }
    }

    /**
     * Registers the given player with a random/known password. Necessary because AuthMe 6's
     * {@code forceLogin} is a no-op for players that do not already have a database account.
     *
     * @param player the player to register
     * @param password the password to use
     * @param autoLogin whether the player should be logged in directly after registration
     */
    public void forceRegister(Player player, String password, boolean autoLogin) {
        Object api = apiInstance();
        if (api == null || forceRegisterMethod == null || player == null) {
            return;
        }
        try {
            forceRegisterMethod.invoke(api, player, password, autoLogin);
        } catch (IllegalAccessException | InvocationTargetException e) {
            plugin.getLogger().warning("Failed to force authme registration for '" + player.getName()
                + "': " + e.getCause());
        }
    }

    public void forceUnregister(Player player) {
        Object api = apiInstance();
        if (api == null || player == null) {
            return;
        }
        try {
            if (forceUnregisterPlayerMethod != null) {
                forceUnregisterPlayerMethod.invoke(api, player);
            } else if (forceUnregisterNameMethod != null) {
                forceUnregisterNameMethod.invoke(api, player.getName());
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            plugin.getLogger().warning("Failed to force authme unregister for '" + player.getName()
                + "': " + e.getCause());
        }
    }

    /**
     * Removes an AuthMe account by player name, even if the player is offline.
     *
     * @param playerName the case-insensitive player name
     */
    public void forceUnregister(String playerName) {
        Object api = apiInstance();
        if (api == null || playerName == null) {
            return;
        }
        try {
            if (forceUnregisterNameMethod != null) {
                forceUnregisterNameMethod.invoke(api, playerName);
            } else {
                Player target = Bukkit.getPlayerExact(playerName);
                if (target != null) {
                    forceUnregister(target);
                } else {
                    plugin.getLogger().warning("AuthMe API has no offline unregister; skipping '" + playerName + "'");
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            plugin.getLogger().warning("Failed to force authme unregister for '" + playerName
                + "': " + e.getCause());
        }
    }

    public Optional<AuthPlayerInfo> getPlayerInfo(String playerName) {
        Object api = apiInstance();
        if (api == null || getPlayerInfoMethod == null) {
            return Optional.empty();
        }
        try {
            Object result = getPlayerInfoMethod.invoke(api, playerName);
            if (result instanceof Optional<?>) {
                Optional<?> opt = (Optional<?>) result;
                return opt.map(AuthmeBridge::toPlayerInfo);
            }
            if (result != null) {
                return Optional.of(toPlayerInfo(result));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            // fall through
        }
        return Optional.empty();
    }

    private static AuthPlayerInfo toPlayerInfo(Object authMePlayer) {
        return new AuthPlayerInfo(
            invokeAnyToString(authMePlayer, "getName"),
            invokeOptionalString(authMePlayer, "getEmail"),
            invokeOptionalString(authMePlayer, "getLastLoginDate"),
            invokeOptionalString(authMePlayer, "getLastLoginIpAddress"),
            invokeOptionalString(authMePlayer, "getRegistrationIpAddress"),
            invokeAnyToString(authMePlayer, "getRegistrationDate"));
    }

    private static String invokeAnyToString(Object target, String methodName) {
        Object value = invokeGetter(target, methodName, Object.class);
        if (value instanceof Optional<?>) {
            value = ((Optional<?>) value).orElse(null);
        }
        return value != null ? value.toString() : null;
    }

    private static <T> T invokeGetter(Object target, String methodName, Class<T> type) {
        try {
            Method m = target.getClass().getMethod(methodName);
            Object value = m.invoke(target);
            return type.isInstance(value) ? type.cast(value) : null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    private static String invokeOptionalString(Object target, String methodName) {
        Object value = invokeGetter(target, methodName, Object.class);
        if (!(value instanceof Optional<?>)) {
            return null;
        }
        Object inner = ((Optional<?>) value).orElse(null);
        return inner != null ? inner.toString() : null;
    }

    /**
     * Read-only projection of AuthMe player data (name / email / dates / IPs) fetched via the API.
     */
    public static final class AuthPlayerInfo {
        private final String name;
        private final String email;
        private final String lastLoginDate;
        private final String lastIp;
        private final String registrationIp;
        private final String registrationDate;

        AuthPlayerInfo(String name, String email, String lastLoginDate, String lastIp,
                       String registrationIp, String registrationDate) {
            this.name = name;
            this.email = email;
            this.lastLoginDate = lastLoginDate;
            this.lastIp = lastIp;
            this.registrationIp = registrationIp;
            this.registrationDate = registrationDate;
        }

        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getLastLoginDate() { return lastLoginDate; }
        public String getLastIp() { return lastIp; }
        public String getRegistrationIp() { return registrationIp; }
        public String getRegistrationDate() { return registrationDate; }
    }
}