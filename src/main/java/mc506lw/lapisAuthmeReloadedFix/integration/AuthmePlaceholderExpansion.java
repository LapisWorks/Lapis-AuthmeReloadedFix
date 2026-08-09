package mc506lw.lapisAuthmeReloadedFix.integration;

import mc506lw.lapisAuthmeReloadedFix.bridge.AuthmeBridge;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

/**
 * PlaceholderAPI expansion exposing %authme_*% variables by reading the AuthMe API.
 * This restores the PlaceholderAPI integration that AuthMe 6 dropped (the old AuthMeExpansion).
 */
public final class AuthmePlaceholderExpansion extends PlaceholderExpansion {

    private final AuthmeBridge authmeBridge;

    public AuthmePlaceholderExpansion(AuthmeBridge authmeBridge) {
        this.authmeBridge = authmeBridge;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "authme";
    }

    @Override
    public @NotNull String getAuthor() {
        return "LapisAuthmeFix";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getRequiredPlugin() {
        return "AuthMe";
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (!authmeBridge.isAvailable()) {
            return null;
        }
        String identifier = params.toLowerCase(Locale.ROOT);
        switch (identifier) {
            case "isloggedin":
                return offlinePlayer instanceof Player ? String.valueOf(authmeBridge.isAuthenticated((Player) offlinePlayer)) : null;
            case "isregistered":
                return offlinePlayer != null ? String.valueOf(authmeBridge.isRegistered(offlinePlayer.getName())) : null;
            case "hasemail":
                return playerInfo(offlinePlayer).map(info -> info.getEmail() == null ? "false" : "true").orElse("false");
            case "email":
                return playerInfo(offlinePlayer).map(info -> info.getEmail() == null ? "" : info.getEmail()).orElse("");
            case "lastlogin":
                return playerInfo(offlinePlayer).map(AuthmeBridge.AuthPlayerInfo::getLastLoginDate).orElse("");
            case "lastip":
                return playerInfo(offlinePlayer).map(AuthmeBridge.AuthPlayerInfo::getLastIp).orElse("");
            case "regdate":
            case "registrationdate":
                return playerInfo(offlinePlayer).map(AuthmeBridge.AuthPlayerInfo::getRegistrationDate).orElse("");
            case "realname":
                return resolvedName(playerInfo(offlinePlayer), offlinePlayer);
            case "debug":
                return authmeBridge.describeBinding()
                    + (offlinePlayer != null ? " / info=" + playerInfo(offlinePlayer).isPresent() : "");
            default:
                return null;
        }
    }

    /**
     * Returns the player's real name, falling back to the offline player's name if AuthMe has nothing.
     */
    private String resolvedName(Optional<AuthmeBridge.AuthPlayerInfo> info, OfflinePlayer offlinePlayer) {
        if (info.isPresent() && info.get().getName() != null && !info.get().getName().isEmpty()) {
            return info.get().getName();
        }
        return offlinePlayer != null ? offlinePlayer.getName() : "";
    }

    private Optional<AuthmeBridge.AuthPlayerInfo> playerInfo(OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null) {
            return Optional.empty();
        }
        return authmeBridge.getPlayerInfo(offlinePlayer.getName());
    }
}