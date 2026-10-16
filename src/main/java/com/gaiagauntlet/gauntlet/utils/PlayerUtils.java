package com.gaiagauntlet.gauntlet.utils;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.auth.ProfileServiceClient;
import com.hypixel.hytale.server.core.auth.ServerAuthManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SpawnUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.gaiagauntlet.gauntlet.core.GauntletUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PlayerUtils {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    // map of local lowercase name to the actual hytale name - cached here so that
    // the same name lookup isn't done more than once
    // this should be persisted somewhere to not get rate limited
    public static final Map<String, String> cachedPlayers = new ConcurrentHashMap<>();
    // second cache to go from normalizedUsername -> UUID
    public static final Map<String, UUID> cachedPlayerIds = new ConcurrentHashMap<>();
    public static final Map<UUID, String> idsToPlayer = new ConcurrentHashMap<>();
    // players with a world move in flight, so a second move cannot pull them out of a store they already left
    private static final Set<UUID> moving = ConcurrentHashMap.newKeySet();

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
        if (playerUuid == null) return null;
        return Universe.get().getPlayer(playerUuid);
    }

    public static World worldOf(PlayerRef player) {
        var world = player.getWorldUuid();
        return Universe.get().getWorld(world);
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
    public static String nameFor(UUID playerUuid) {
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

    /** Clear inventory, set to adventure, and restore health. */
    /** Completes once the player holds a valid ref in the hub, or exceptionally when they never arrive. */
    @Nonnull
    public static CompletableFuture<Void> sendToHub(@Nonnull PlayerRef player) {
        var hub = GauntletUtils.withHubWorld();
        return SpawnUtil.resolveSpawnPoint(hub, player.getUuid())
                .thenCompose(spawn -> moveToWorld(player, hub, spawn, null));
    }

    /**
     * Moves a player into another world the same way {@link World#drainPlayersTo} does: they are pulled out of
     * their current store and added to the destination. The move is queued on the origin world so it runs
     * between ticks, never while that world's systems still hold the player's ref. Only one move per player can
     * be in flight, and a player the destination refuses is sent back to the hub.
     *
     * @param prepare runs on the player's holder once they have left the origin world, to attach components
     *                that should travel with them
     */
    public static CompletableFuture<Void> moveToWorld(@Nonnull PlayerRef player, @Nonnull World destination,
            @Nonnull Transform transform, @Nullable Consumer<Holder<EntityStore>> prepare) {
        var landed = new CompletableFuture<Void>();
        var ref = player.getReference();
        if (ref == null) {
            landed.completeExceptionally(new IllegalStateException(player.getUsername() + " is not in a world"));
            return landed;
        }
        if (!moving.add(player.getUuid())) {
            landed.completeExceptionally(
                    new IllegalStateException(player.getUsername() + " is already moving between worlds"));
            return landed;
        }
        landed.whenComplete((_, _) -> moving.remove(player.getUuid()));

        var origin = ref.getStore().getExternalData().getWorld();
        try {
            origin.execute(() -> {
                try {
                    if (player.getReference() != ref) {
                        landed.completeExceptionally(new IllegalStateException(
                                player.getUsername() + " left " + origin.getName() + " before the move"));
                        return;
                    }
                    if (!destination.isAlive()) {
                        landed.completeExceptionally(
                                new IllegalStateException(destination.getName() + " is no longer running"));
                        return;
                    }
                    var holder = player.removeFromStore();
                    if (prepare != null) {
                        prepare.accept(holder);
                    }
                    var added = destination.addPlayer(player, transform);
                    if (added == null) {
                        landed.completeExceptionally(
                                new IllegalStateException(player.getUsername() + " disconnected during the move"));
                        return;
                    }
                    added.whenComplete((_, error) -> {
                        if (error == null) {
                            landed.complete(null);
                        } else {
                            rescue(player, destination, error, landed);
                        }
                    });
                } catch (Exception error) {
                    rescue(player, destination, error, landed);
                }
            });
        } catch (Exception error) {
            landed.completeExceptionally(error);
        }
        return landed;
    }

    /**
     * Fails a move, first putting the player back in the hub if the move left them out of every world. Players
     * who cannot be placed anywhere are disconnected rather than left frozen.
     */
    private static void rescue(PlayerRef player, World failed, Throwable error, CompletableFuture<Void> landed) {
        if (player.getReference() != null || player.getHolder() == null) {
            landed.completeExceptionally(error);
            return;
        }
        var hub = GauntletUtils.withHubWorld();
        if (hub == failed || !hub.isAlive()) {
            player.getPacketHandler().disconnect(Message.raw("Could not move you between worlds"));
            landed.completeExceptionally(error);
            return;
        }
        SpawnUtil.resolveSpawnPoint(hub, player.getUuid())
                .thenCompose(spawn -> Objects.requireNonNullElseGet(hub.addPlayer(player, spawn),
                        () -> CompletableFuture.completedFuture(null)))
                .whenComplete((_, rescueError) -> {
                    if (rescueError != null) {
                        error.addSuppressed(rescueError);
                        player.getPacketHandler().disconnect(Message.raw("Could not move you between worlds"));
                    }
                    landed.completeExceptionally(error);
                });
    }

    public static void reset(PlayerRef playerRef) {
        var ref = playerRef.getReference();
        if (ref == null) return;
        var accessor = ref.getStore();

        // clear inventory
        InventoryComponent
            .getCombined(accessor, ref, InventoryComponent.EVERYTHING)
            .clear();

        // EVERYTHING does not include tools, so we clear them separately
        final var toolComponent = accessor.getComponent(ref, InventoryComponent.Tool.getComponentType());
        if (toolComponent != null)
            toolComponent.getInventory().clear();

        Player.setGameMode(ref, GameMode.Adventure, accessor);

        // restore health
        EntityStatMap stats = accessor.getComponent(ref, EntityStatMap.getComponentType());
        if (stats != null) {
            stats.maximizeStatValue(DefaultEntityStatTypes.getHealth());
        }
    }
}
