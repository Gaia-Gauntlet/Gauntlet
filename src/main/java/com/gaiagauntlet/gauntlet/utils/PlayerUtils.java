package com.gaiagauntlet.gauntlet.utils;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.auth.ProfileServiceClient;
import com.hypixel.hytale.server.core.auth.ServerAuthManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerUtils {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    // map of local lowercase name to the actual hytale name - cached here so that
    // the same name lookup isn't done more than once
    // this should be persisted somewhere to not get rate limited
    public static final Map<String, String> cachedPlayers = new ConcurrentHashMap<>();
    // second cache to go from normalizedUsername -> UUID
    public static final Map<String, UUID> cachedPlayerIds = new ConcurrentHashMap<>();
    public static final Map<UUID, String> idsToPlayer = new ConcurrentHashMap<>();

    private PlayerUtils() {
    }

    @Nonnull
    public static String normalize(@Nonnull String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isOnline(@Nonnull UUID uuid) {
        return Objects.nonNull(Universe.get().getPlayer(uuid));
    }

    /**
     * Records the final username to the cache - the passed username MUST be the
     * exact casing of the actual username
     */
    public static void record(@Nonnull String username, @Nonnull UUID uuid) {
        var normalized = normalize(username);
        cachedPlayers.put(normalized, username);
        cachedPlayerIds.put(normalized, uuid);
        idsToPlayer.put(uuid, normalized);
    }

    @Nullable
    public static PlayerRef get(UUID playerUuid) {
        return Universe.get().getPlayer(playerUuid);
    }

    /**
     * Returning null means the player is offline
     * @param playerName Name of the player to fetch the UUID of
     * @return
     */
    @Nullable
    public static CompletableFuture<UUID> uuidOf(String playerName) {
        if (playerName == null || playerName.length() <= 1) return CompletableFuture.completedFuture(null);
        var normalisedName = normalize(playerName);
        var uuid = cachedPlayerIds.get(playerName);
        if (uuid != null) return CompletableFuture.completedFuture(uuid);

        var uuidFuture = new CompletableFuture<UUID>();
        var future = resolve(normalisedName);
        future.whenComplete((name, error) -> {
            if (error != null) {
                uuidFuture.complete(null);
                return;
            }
            if (name == null) {
                uuidFuture.complete(null);
            }

            // get it again - it should be there now
            var playerUuid = cachedPlayerIds.get(normalize(name));
            uuidFuture.complete(playerUuid);
        });
        return uuidFuture;
    }

    /**
     * Resolves a player's actual username if they are online - otherwise null
     */
    @Nullable
    public static String resolveOnline(String username) {
        var cachedPlayerName = cachedPlayers.get(normalize(username));
        if (cachedPlayerName != null) return cachedPlayerName;

        // check the universe
        var player = Universe.get().getPlayerByUsername(username, NameMatching.EXACT_IGNORE_CASE);
        if (Objects.isNull(player)) return null;
        var playerUsername = player.getUsername();
        // record it for future reference / caching
        record(playerUsername, player.getUuid());
        return playerUsername;
    }
    
    @Nullable
    public static String resolveOnline(UUID playerUuid) {
        var cachedNormalized = idsToPlayer.get(playerUuid);
        if (cachedNormalized != null) {
            return resolveOnline(cachedNormalized);
        }

        // check the universe
        var player = Universe.get().getPlayer(playerUuid);
        if (player == null) return null;
        var playerUsername = player.getUsername();
        // record it for future reference / caching
        record(playerUsername, playerUuid);
        return playerUsername;
    }

    public static CompletableFuture<String> resolve(String username) {
        var user = resolveOnline(username);
        if (user != null) return CompletableFuture.completedFuture(user);

        var auth = ServerAuthManager.getInstance();
        var token = auth.getSessionToken();
        if (token == null) return CompletableFuture.completedFuture(null);

        var client = auth.getProfileServiceClient();
        return resolve(username, client, token);
    }

    public static CompletableFuture<String> resolve(String username, ProfileServiceClient client, String token) {
        var nameFuture = new CompletableFuture<String>();
        var cachedPlayerName = cachedPlayers.get(normalize(username));
        if (cachedPlayerName != null) {
            nameFuture.complete(cachedPlayerName);
            return nameFuture;
        }
        client.getProfileByUsernameAsync(username, token).whenComplete((profile, error) -> {
            if (error != null || profile == null || profile.getUuid() == null) {
                LOGGER.atWarning().log("No account found for %s", username);
                nameFuture.complete(null);
                return;
            }
            record(profile.getUsername(), profile.getUuid());
            nameFuture.complete(profile.getUsername());
        });
        return nameFuture;
    }

    public static void resolve(@Nonnull Collection<String> usernames) {
        var auth = ServerAuthManager.getInstance();
        var token = auth.getSessionToken();
        if (token == null) {
            return;
        }
        var client = auth.getProfileServiceClient();
        for (var username : usernames) {
            var user = resolveOnline(username);
            if (user != null)
                continue; // player is online, skip the auth process
            resolve(username, client, token);
        }
    }
}
