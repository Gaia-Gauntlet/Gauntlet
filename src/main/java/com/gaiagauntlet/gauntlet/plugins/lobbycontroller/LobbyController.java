package com.gaiagauntlet.gauntlet.plugins.lobbycontroller;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
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
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.transfer.TransferUtils;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/** Should enforce the implementation of a Lobby-Arena system */
public abstract class LobbyController extends GameController {
    public static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public abstract LobbyManager getLobbyManager();

    public abstract ArenaManager getArenaManager();

    public abstract void setupGame(World world, GameEcs gameStore, String sessionId);

    private Optional<World> withArenaWorld(World hubWorld, String sessionId) {
        return withArenaWorld(hubWorld.getEntityStore().getStore(), sessionId);
    }

    private Optional<World> withArenaWorld(ComponentAccessor<EntityStore> hubAccessor, String sessionId) {
        if (!(GameStore.withStore(hubAccessor, sessionId).orElse(null) instanceof GameEcs hubStore)) {
            return Optional.empty();
        }

        var existing = hubStore.get(LobbyComponent.getComponentType());
        if (!existing.isPresent())
            return Optional.empty();

        return Optional.of(existing.get().getWorld()).filter(world -> world.isAlive());
    }

    /**
     * Sets up the game and wraps `setupGame` for the underlying controller to
     * implement. Creates the lobby world and preps things for players to start
     * joining
     */
    @Override
    public final CompletableFuture<Void> setupGame(ComponentAccessor<EntityStore> hubAccessor, GameSession session) {
        var sessionId = session.getId();
        if (!(GameStore.ensureStore(hubAccessor, sessionId) instanceof GameEcs hubStore)) {
            return CompletableFuture.completedFuture(null);
        }
        var existing = hubStore.get(LobbyComponent.getComponentType());
        if (existing.isPresent()) {
            GaiaLog.atWarning().withSession(session)
                    .log("Initializing " + getId() + " with session " + sessionId
                            + " where a game was already initialized! Previous game did not shut down correctly. Clearing and continuing");
            hubStore.clear();
        }

        return getLobbyManager().setupWorld(hubAccessor, sessionId).thenCompose(world -> {
            GauntletUtils.run(hubAccessor.getExternalData().getWorld(), () -> {
                // hop to the hub thread again to finalize the initialization of the component
                hubStore.put(LobbyComponent.getComponentType(), new LobbyComponent(world));
            });
            // ensure that a weird world doesn't throw us into an odd thread, hop into the
            // world thread
            return GauntletUtils.runAsync(world, () -> {
                var lobbyStore = world.getEntityStore().getStore();
                var gameStore = GameStore.withResource(world).create(sessionId);

                // register the plugins that have persistence
                var persistentPlugins = GameRegistry.getPlugins(getRequiredPlugins(), PersistentGamePlugin.class);
                for (var plugin : persistentPlugins) {
                    try {
                        plugin.setup(lobbyStore, session, gameStore, getId());
                    } catch (Exception e) {
                        GaiaLog.atWarning().withCause(e).withSession(session)
                                .log("Persistent Plugin " + plugin.getId() + " failed while loading for " + getId());
                    }
                }

                var simplePlugins = GameRegistry.getPlugins(getRequiredPlugins(), SimpleGamePlugin.class);
                for (var plugin : simplePlugins) {
                    try {
                        plugin.setup(lobbyStore, getId());
                    } catch (Exception e) {
                        GaiaLog.atWarning().withCause(e).withSession(session)
                                .log("Simple Plugin " + plugin.getId() + " failed while loading for " + getId());

                    }
                }

                setupGame(world, gameStore, sessionId);
            });
        });
    };

    @Override
    public CompletableFuture<Void> playerJoin(World hubWorld, String sessionId, Collection<PlayerRef> players) {
        var store = hubWorld.getEntityStore().getStore();
        // TODO: Get the defaut transform location for players. the onConnect should
        // handle moving the player to the right location after warping
        var arena = withArenaWorld(hubWorld, sessionId).filter(World::isAlive).orElse(null);

        if (arena == null)
            return CompletableFuture.failedFuture(new IllegalStateException("No live arena for session " + sessionId));

        TransferUtils.queue(store, arena, sessionId, new HashSet<>(players), new Transform());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public final CompletableFuture<Void> cleanGame(World hubAccessor, GameSession session) {
        var arenaWorld = withArenaWorld(hubAccessor, session.getId()).orElse(null);
        if (arenaWorld == null)
            return CompletableFuture.completedFuture(null);
        return getLobbyManager().cleanWorld(arenaWorld);
    };

    @Override
    public CompletableFuture<Void> playerDisconnect(World hubWorld, GameSession session, PlayerRef player) {
        var arenaWorld = withArenaWorld(hubWorld, session.getId()).orElse(null);
        if (arenaWorld == null)
            throw new IllegalStateException("Arena world does not exist!");

        return GauntletUtils.runAsync(arenaWorld, () -> {
            getLobbyManager().onDisconnect(arenaWorld, session.getId(), player);
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> playerLeave(World hubWorld, String sessionId, Collection<PlayerRef> player) {
        var arenaWorld = withArenaWorld(hubWorld, sessionId).orElse(null);
        if (arenaWorld == null)
            throw new IllegalStateException("Arena world does not exist!");

        return GauntletUtils.runAsync(arenaWorld, () -> {
            getLobbyManager().onLeave(arenaWorld, sessionId, player);
            return null;
        });
    }
}
