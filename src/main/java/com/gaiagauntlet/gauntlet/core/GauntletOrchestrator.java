package com.gaiagauntlet.gauntlet.core;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.admin.AdminLog;
import com.gaiagauntlet.gauntlet.core.components.PlayerComponent;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.resources.UniverseGameResource;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.core.session.constants.SessionState;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

/**
 * The very thin big boi router
 * 90% of the business logic for this should exist within the GameController
 * 
 * The Orchestrator is simply there to route and standardize implementations
 */
public class GauntletOrchestrator {

    /**
     * Sets up a game to allow for sending players to and, later, starting the game
     * itself
     */
    public static CompletableFuture<GameController> setupGame(String sessionId) {
        var sessionRes = sessionFor(sessionId);
        if (!sessionRes.isPresent() || !sessionRes.get().available()) {
            AdminLog.add("Unable to setup the session's game. The session is not in a valid state!");
            return null;
        }
        var session = sessionRes.get();

        var nextGameId = session.getNext();
        var gameRes = GameRegistry.getGame(nextGameId);
        if (!gameRes.isPresent()) {
            AdminLog.add("Game " + nextGameId + " is not registered, cannot set up!");
            return null;
        }
        var game = gameRes.get();

        // pass stuff to the game
        game.setupGame();

        // ensure this runs AFTER the game is made, needs to finalize what the game actually needs in order to be created
        return CompletableFuture.completedFuture(game);
    }

    /** ----- Utilities and shortcuts to make stuff faster ----- */

    /** Shortcut for getting the universe resource with all the sessions */
    public static UniverseGameResource withResource() {
        return Universe.get().getResource(UniverseGameResource.getResourceType());
    }

    @Nonnull
    public static Optional<GameSession> sessionFor(@Nonnull String id) {
        return withResource().getSession(id);
    }

    @Nonnull
    public static Optional<GameSession> sessionFor(@Nullable PlayerRef player) {
        var comp = playerFor(player);
        if (comp.isPresent() && comp.get().getActiveSession() != null)
            return sessionFor(comp.get().getActiveSession());
        return Optional.empty();
    }

    @Nonnull
    public static Optional<PlayerComponent> playerFor(@Nullable PlayerRef player) {
        return Optional.ofNullable(player.getComponentConcurrent(PlayerComponent.getComponentType()));
    }
}
