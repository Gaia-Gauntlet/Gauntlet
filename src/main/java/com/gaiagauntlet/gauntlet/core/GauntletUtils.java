package com.gaiagauntlet.gauntlet.core;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.components.PlayerComponent;
import com.gaiagauntlet.gauntlet.core.config.GauntletConfig;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.gaiagauntlet.gauntlet.core.resources.UniverseGauntletResource;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

/**
 * Stateless getters or setters
 */
public class GauntletUtils {
    /** Shortcut for getting the universe resource with all the sessions */
    public static UniverseGauntletResource withResource() {
        return Universe.get().getResource(UniverseGauntletResource.getResourceType());
    }

    @Nonnull
    public static Optional<GameSession> sessionFor(@Nonnull String sessionId) {
        return withResource().getSession(sessionId);
    }

    @Nonnull
    public static Optional<GameSession> sessionFor(@Nonnull PlayerRef player) {
        var party = PartyUtils.getParty(player).orElse(null);
        if (party == null)
            return Optional.empty();
        return PartyUtils.sessionFor(party.getId());
    }

    @Nonnull
    public static Optional<PlayerComponent> playerFor(@Nonnull UUID playerId) {
        var playerRef = Universe.get().getPlayer(playerId);
        if (playerRef == null)
            return Optional.empty();
        return playerFor(playerRef);
    }

    @Nonnull
    public static Set<PlayerRef> playersFor(@Nonnull String sessionId) {
        var session = sessionFor(sessionId).orElse(null);
        return playersFor(session);
    }

    @Nonnull
    public static Set<PlayerRef> playersFor(@Nonnull GameSession session) {
        var players = new HashSet<PlayerRef>();
        if (session == null)
            return players;

        var parties = session.getParties();
        for (var partyId : parties) {
            PartyUtils.getParty(partyId).ifPresent(party -> {
                players.addAll(party.getAllOnlinePlayers());
            });
        }
        return players;
    }

    public static void updatePlayer(@Nonnull PlayerRef player, @Nonnull Consumer<PlayerComponent> change) {
        var ref = player.getReference();
        if (ref == null || !ref.isValid())
            return;
        run(ref.getStore().getExternalData().getWorld(), () -> {
            if (!ref.isValid())
                return;
            var component = ref.getStore().getComponent(ref, PlayerComponent.getComponentType());
            if (component != null)
                try {
                    change.accept(component);
                } catch (Exception e) {
                    GaiaLog.atError(e).log("Failed to run player mutation to " + player.getUsername());
                }
        });
    }

    @Nonnull
    public static Optional<PlayerComponent> playerFor(@Nonnull PlayerRef player) {
        return Optional.ofNullable(player.getComponentConcurrent(PlayerComponent.getComponentType()));
    }

    /** Gets the world of the hub, falls back to default universe world */
    public static World withHubWorld() {
        var hubId = GauntletConfig.get().getHubId();
        if (hubId != null) {
            var hubWorld = Universe.get().getWorld(GauntletConfig.get().getHubId());
            if (hubWorld != null)
                return hubWorld;
        }
        return Universe.get().getDefaultWorld();
    }

    /** Returns a completable future on the thread */
    public static <T> CompletableFuture<T> runAsync(World world, Supplier<T> operation) {
        var future = new CompletableFuture<T>();
        Runnable task = () -> {
            try {
                future.complete(operation.get());
            } catch (Exception e) {
                GaiaLog.atError(e).log("Failed to run wrapped function");
                future.completeExceptionally(e);
            }
        };
        try {
            run(world, task);
        } catch (Exception e) {
            GaiaLog.atError(e).log("World " + (world == null ? "none" : world.getName()) + " refused a task");
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * Returns a completable future on the world thread with no return type needed
     */
    public static CompletableFuture<Void> runAsync(World world, Runnable operation) {
        return runAsync(world, () -> {
            operation.run();
            return null;
        });
    }

    /** Runs on the thread with no try-catching surrounding the task */
    public static void run(@Nullable World world, Runnable task) {
        if (world == null) {
            task.run();
        } else if (world.isInThread()) {
            task.run();
        } else {
            world.execute(task);
        }
    }
}
