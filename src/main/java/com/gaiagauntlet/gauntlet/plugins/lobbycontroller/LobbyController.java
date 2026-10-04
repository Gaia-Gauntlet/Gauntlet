package com.gaiagauntlet.gauntlet.plugins.lobbycontroller;

import java.util.concurrent.CompletableFuture;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.interfaces.SimpleGamePlugin;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.PersistentGamePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.components.LobbyComponent;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.ArenaManager;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.LobbyManager;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/** Should enforce the implementation of a Lobby-Arena system */
public abstract class LobbyController extends GameController {
    public static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public abstract LobbyManager getLobbyManager();

    public abstract ArenaManager getArenaManager();

    public abstract void setupGame(World world, GameEcs gameStore, String sessionId);

    /**
     * Sets up the game and wraps `setupGame` for the underlying controller to
     * implement. Creates the lobby world and preps things for players to start
     * joining
     */
    @Override
    public final CompletableFuture<Void> setupGame(ComponentAccessor<EntityStore> hubAccessor, GameSession session) {
        var sessionId = session.getId();
        if (!(GameStore.withStore(hubAccessor, sessionId).orElse(null) instanceof GameEcs hubStore)) {
            return CompletableFuture.completedFuture(null);
        }
        var existing = hubStore.get(LobbyComponent.getComponentType());
        if (existing.isPresent()) {
            LOGGER.atWarning().log(
                    "Initializing %s with session %s where a game was already initialized! Previous game did not shut down correctly. Clearing and continuing",
                    getId(), sessionId);
            hubStore.clear();
        }

        // register the early plugins
        var persistentPlugins = GameRegistry.getPlugins(getRequiredPlugins(), PersistentGamePlugin.class);
        for (var plugin : persistentPlugins) {
            try {
                plugin.setup(hubAccessor, session, getId());
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Persistent Plugin %s failed while loading for %s", plugin.getId(),
                        getId());
            }
        }

        return getLobbyManager().setupWorld().thenCompose(world -> {
            hubAccessor.getExternalData().getWorld().execute(() -> {
                // hop to the hub thread again to finalize the initialization of the component
                hubStore.put(LobbyComponent.getComponentType(), new LobbyComponent(world));

            });
            // ensure that a weird world doesn't throw us into an odd thread, hop into the
            // world thread
            return onWorld(world, () -> {
                var lobbyStore = world.getEntityStore().getStore();
                var gameStore = GameStore.withResource(world).create(sessionId);

                var simplePlugins = GameRegistry.getPlugins(getRequiredPlugins(), SimpleGamePlugin.class);
                for (var plugin : simplePlugins) {
                    try {
                        plugin.setup(lobbyStore, getId());
                    } catch (Exception e) {
                        LOGGER.atWarning().withCause(e).log("Simple Plugin %s failed while loading for %s",
                                plugin.getId(), getId());
                    }
                }

                setupGame(world, gameStore, sessionId);
            });
        })
                .whenComplete((ignored, error) -> {
                    if (error != null) {
                        LOGGER.atSevere()
                                .withCause(error)
                                .log("Failed to initialize game %s for session %s", getId(), sessionId);
                        // Mark session ERROR and begin cleanup.
                        // TODO: add eventing so that the admin GUI can be notified of the errored state
                        // and take recovery action
                    }
                });
    };

    @Override
    public final CompletableFuture<Void> cleanGame(World hubAccessor, GameSession session) {
        // remove the game
        return CompletableFuture.completedFuture(null);
    };

    @Override
    public final CompletableFuture<Void> playerJoin(World hubAccessor, String sessionId,
            PlayerRef player) {
        // add a player to the game
        return CompletableFuture.completedFuture(null);
    };

    @Override
    public final CompletableFuture<Void> playerLeave(World hubAccessor, String sessionId,
            PlayerRef player) {
        return CompletableFuture.completedFuture(null);
        // remove a player from the game
    };

    private CompletableFuture<Void> onWorld(World world, Runnable runner) {
        var result = new CompletableFuture<Void>();

        Runnable guarded = () -> {
            try {
                runner.run();
                result.complete(null);
            } catch (Exception exception) {
                result.completeExceptionally(exception);
            }
        };

        try {
            if (world.isInThread()) {
                guarded.run();
                result.complete(null);
            } else {
                world.execute(guarded);
            }
        } catch (Exception exception) {
            result.completeExceptionally(exception);
        }

        return result;
    }
}
