package com.gaiagauntlet.gauntlet.plugins.lobbycontroller;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.interfaces.SimpleGamePlugin;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.PersistentGamePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.components.LobbyComponent;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.events.ArenaLoadedEvent;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.ArenaManager;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.LobbyManager;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.transfer.TransferUtils;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.Pair;

/** Should enforce the implementation of a Lobby-Arena system */
public abstract class LobbyController extends GameController {
    public static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public abstract LobbyManager getLobbyManager();

    public abstract ArenaManager getArenaManager();

    public abstract CompletableFuture<Void> setupGame(World world, GameEcs gameStore, String sessionId);

    public static Optional<World> withArenaWorld(String sessionId) {
        return withArenaWorld(GauntletUtils.sessionFor(sessionId).orElse(null));
    }

    public static Optional<World> withArenaWorld(GameSession session) {

        if (session == null)
            return Optional.empty();

        var existing = session.get(LobbyComponent.getComponentType());
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
        var existing = session.get(LobbyComponent.getComponentType());
        if (existing.isPresent()) {
            GaiaLog.atWarning().withSession(session)
                    .log("Initializing " + getId() + " with session " + sessionId
                            + " where a game was already initialized! Previous game did not shut down correctly. Clearing and continuing");
            var stale = existing.get().getWorld();
            if (stale.isAlive())
                getLobbyManager().cleanWorld(stale);
        }

        var attempt = session.getGeneration();
        var hubWorld = hubAccessor.getExternalData().getWorld();
        return getLobbyManager().setupWorld(hubAccessor, sessionId).thenCompose(world -> {
            return GauntletUtils.runAsync(hubWorld, () -> {
                if (!session.isCurrent(attempt)) {
                    getLobbyManager().cleanWorld(world);
                    return CompletableFuture.<Void>failedFuture(
                            new CancellationException("Setup attempt " + attempt + " of " + sessionId + " was abandoned"));
                }
                session.put(LobbyComponent.getComponentType(), new LobbyComponent(world));
                return prepareArena(world, session);
            }).thenCompose(prepared -> prepared);
        });
    };

    private CompletableFuture<Void> prepareArena(World world, GameSession session) {
        var sessionId = session.getId();
        return GauntletUtils.runAsync(world, () -> {
            // an arena left behind by a crash has no session pointing at it, so the engine deletes it on the next boot
            var worldConfig = world.getWorldConfig();
            worldConfig.setDeleteOnUniverseStart(true);
            worldConfig.markChanged();

            var lobbyStore = world.getEntityStore().getStore();
            var gameStore = GameStore.ensureStore(world, sessionId);

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
                    plugin.setup(lobbyStore, sessionId, getId());
                } catch (Exception e) {
                    GaiaLog.atWarning().withCause(e).withSession(session)
                            .log("Simple Plugin " + plugin.getId() + " failed while loading for " + getId());

                }
            }

            // dispatch event
            MatchEventRegistry.dispatch(new ArenaLoadedEvent(sessionId, world), getId());

            // players are only sent in once the game has finished preparing its arena
            return setupGame(world, gameStore, sessionId);
        }).thenCompose(prepared -> prepared);
    }

    @Override
    public CompletableFuture<Void> playerJoin(World hubWorld, String sessionId, Collection<PlayerRef> players) {
        var store = hubWorld.getEntityStore().getStore();
        // TODO: Get the defaut transform location for players. the onConnect should
        // handle moving the player to the right location after warping
        var arena = withArenaWorld(sessionId).filter(World::isAlive).orElse(null);

        if (arena == null)
            return CompletableFuture.failedFuture(new IllegalStateException("No live arena for session " + sessionId));

        var arenaId = arena.getWorldConfig().getUuid();
        var playerSets = players.stream()
                .filter(player -> !arenaId.equals(player.getWorldUuid()))
                .map(player -> Pair.of(player, getLobbyManager().locationFor(player, arena, sessionId))).toList();

        TransferUtils.queue(store, arena, sessionId, playerSets);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public final CompletableFuture<Void> cleanGame(World hubAccessor, GameSession session) {
        var arenaWorld = withArenaWorld(session.getId()).orElse(null);
        if (arenaWorld == null)
            return CompletableFuture.completedFuture(null);
        return getLobbyManager().cleanWorld(arenaWorld).whenComplete((data, err) -> GauntletUtils.runAsync(hubAccessor,
                () -> session.remove(LobbyComponent.getComponentType())));
    };

    @Override
    public CompletableFuture<Void> playerLeave(World hubWorld, String sessionId, Collection<PlayerRef> players,
            boolean forfeit) {
        var arenaWorld = withArenaWorld(sessionId).orElse(null);
        if (arenaWorld == null)
            return CompletableFuture.completedFuture(null);

        return GauntletUtils.runAsync(arenaWorld,
                () -> getLobbyManager().onLeave(hubWorld, arenaWorld, sessionId, players, forfeit))
                .thenCompose(future -> future);
    }

    @Override
    public CompletableFuture<Collection<PlayerRef>> playerRevive(World hubWorld, String sessionId,
            Collection<PlayerRef> players) {
        var arenaWorld = withArenaWorld(sessionId).orElse(null);
        if (arenaWorld == null)
            return CompletableFuture.failedFuture(new IllegalStateException("No live arena for session " + sessionId));

        return GauntletUtils.runAsync(arenaWorld, () -> getLobbyManager().onRevive(arenaWorld, sessionId, players));
    }
}
