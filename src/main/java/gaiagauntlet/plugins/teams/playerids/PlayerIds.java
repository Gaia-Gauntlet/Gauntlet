package gaiagauntlet.plugins.teams.playerids;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.auth.ServerAuthManager;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.Config;
import gaiagauntlet.plugins.teams.team.Team;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;

/**
 * Account UUIDs by username, so a HUD can show a player's portrait while they are offline. Learned
 * from every join and, on a server signed in to the account services, looked up by name for the
 * roster. Saved on change.
 */
public final class PlayerIds {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static PlayerIds instance;

    private final Config<PlayerIdsFile> config;

    public PlayerIds(@Nonnull Config<PlayerIdsFile> config) {
        this.config = config;
        instance = this;
    }

    @Nonnull
    public static PlayerIds get() {
        return instance;
    }

    public synchronized void load() {
        var loaded = config.load().join();
        LOGGER.atInfo().log("Loaded %d known player ids", loaded.players().size());
    }

    /** The player's account UUID: from their connection when online, else as last learned, else null. */
    @Nullable
    public UUID uuidOf(@Nonnull String username) {
        var online = Universe.get().getPlayerByUsername(username, NameMatching.EXACT_IGNORE_CASE);
        if (online != null) {
            return online.getUuid();
        }
        String known;
        synchronized (this) {
            known = config.get().players().get(Team.normalize(username));
        }
        if (known == null) {
            return null;
        }
        try {
            return UUID.fromString(known);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public synchronized void record(@Nonnull String username, @Nonnull UUID uuid) {
        var previous = config.get().players().put(Team.normalize(username), uuid.toString());
        if (!uuid.toString().equals(previous)) {
            config.save().exceptionally(e -> {
                LOGGER.atSevere().withCause(e).log("Could not save player ids");
                return null;
            });
        }
    }

    /**
     * Looks up every name not already known through the profile service, in the background. Does
     * nothing on a server without a session, such as one in insecure auth mode.
     */
    public void resolve(@Nonnull Collection<String> usernames) {
        var auth = ServerAuthManager.getInstance();
        var token = auth.getSessionToken();
        if (token == null) {
            return;
        }
        var client = auth.getProfileServiceClient();
        for (var username : usernames) {
            synchronized (this) {
                if (config.get().players().containsKey(Team.normalize(username))) {
                    continue;
                }
            }
            client.getProfileByUsernameAsync(username, token).whenComplete((profile, error) -> {
                if (error != null || profile == null || profile.getUuid() == null) {
                    LOGGER.atFine().log("No account found for %s", username);
                    return;
                }
                record(username, profile.getUuid());
            });
        }
    }
}
