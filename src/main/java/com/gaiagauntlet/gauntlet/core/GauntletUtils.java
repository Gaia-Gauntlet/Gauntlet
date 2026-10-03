package com.gaiagauntlet.gauntlet.core;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.core.admin.AdminLog;
import com.gaiagauntlet.gauntlet.core.components.PlayerComponent;
import com.gaiagauntlet.gauntlet.core.config.GauntletConfig;
import com.gaiagauntlet.gauntlet.core.resources.UniverseGameResource;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

/**
 * Stateless getters or setters
 */
public class GauntletUtils {
    /** Shortcut for getting the universe resource with all the sessions */
    public static UniverseGameResource withResource() {
        return Universe.get().getResource(UniverseGameResource.getResourceType());
    }

    @Nonnull
    public static Optional<GameSession> sessionFor(@Nonnull String id) {
        return withResource().getSession(id);
    }

    @Nonnull
    public static Optional<GameSession> sessionFor(@Nonnull PlayerRef player) {
        var comp = playerFor(player);
        if (comp.isPresent() && comp.get().getActiveSession() != null)
            return sessionFor(comp.get().getActiveSession());
        return Optional.empty();
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
                AdminLog.add("Failed to run wrapped function with error: "
                        + e.getLocalizedMessage());
                future.completeExceptionally(e);
            }
        };
        run(world, task);
        return future;
    }

    /** Returns a completable future on the world thread with no return type needed */
    public static CompletableFuture<Void> runAsync(World world, Runnable operation) {
        return runAsync(world, () -> {
            operation.run();
            return null;
        });
    }

    /** Runs on the thread with no try-catching surrounding the task */
    public static void run(World world, Runnable task) {
        if (world.isInThread()) {
            task.run();
        } else {
            world.execute(task);
        }
    }
}
