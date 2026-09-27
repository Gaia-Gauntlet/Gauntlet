package gaiagauntlet.plugins.settings.utils;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import gaiagauntlet.plugins.settings.constants.Settings;

import javax.annotation.Nonnull;
import java.util.Locale;

public class PermissionsUtils {
    /** True when the player is named in the GG admins setting. */
    public static boolean isGgAdmin(@Nonnull PlayerRef player) {
        var username = player.getUsername().toLowerCase(Locale.ROOT);
        for (var name : Settings.get().get(Settings.ADMIN_PLAYERS).split(",")) {
            if (name.trim().toLowerCase(Locale.ROOT).equals(username)) {
                return true;
            }
        }
        return false;
    }
}
