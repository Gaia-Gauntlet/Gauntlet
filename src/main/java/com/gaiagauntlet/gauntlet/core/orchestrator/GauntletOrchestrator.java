package com.gaiagauntlet.gauntlet.core.orchestrator;

import java.util.concurrent.CompletableFuture;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.AdminLog;
import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.core.events.GauntletEvents;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.orchestrator.handlers.GameHandlers;
import com.gaiagauntlet.gauntlet.core.orchestrator.handlers.PlayerHandlers;
import com.gaiagauntlet.gauntlet.core.orchestrator.handlers.SessionHandlers;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * The very thin big boi router
 * 90% of the business logic for this should exist within the GameController
 * 
 * The Orchestrator is simply there to route and standardize implementations
 * 
 * All business-logic implementations should be within the handlers/
 */
public class GauntletOrchestrator {

    /**
     * Business rules because I have nowhere else to put them.
     * 1) All controller methods should be invoked on the HUB thread
     * 2) SessionId states should be cleared between games
     * 3) Games should be setup before any player is allowed to join
     * 4) Eventing needs
     *     a) Failure Events
     *     b) Session status updates
     * 5) threading needs
     *     a) player join
     *     b) player leave
     *     c) server shutdown
     *     d) server startup (load up from crashed server - attempt recovery?)
     */

    public static void setupListeners() {
        // session event handling
        GauntletEvents.on(GauntletEvent.Session.class, SessionHandlers::handleSession);
        GauntletEvents.on(GauntletEvent.SessionQueue.class, SessionHandlers::handleSessionQueue);
        GauntletEvents.on(GauntletEvent.NewSession.class, SessionHandlers::handleNewSession);
        
        // game event handling
        GauntletEvents.on(GauntletEvent.Game.class, GameHandlers::handleGame);
        GauntletEvents.on(GauntletEvent.GameEnd.class, GameHandlers::handleGameEnd);
        
        // player event handling
        GauntletEvents.on(GauntletEvent.ConnectPlayer.class, PlayerHandlers::handleConnectPlayer);
        GauntletEvents.on(GauntletEvent.DisconnectPlayer.class, PlayerHandlers::handleDisconnectPlayer);
        GauntletEvents.on(GauntletEvent.AddPlayer.class, PlayerHandlers::handleAddPlayer);
        GauntletEvents.on(GauntletEvent.RemovePlayer.class, PlayerHandlers::handleRemovePlayer);
    }

    /**
     * Sets up a game to allow for sending players to and, later, starting the game
     * itself
     */
    public static CompletableFuture<GameController> setupGame(ComponentAccessor<EntityStore> accessor,
            String sessionId) {
        var sessionRes = GauntletUtils.sessionFor(sessionId);
        if (!sessionRes.isPresent() || !sessionRes.get().available()) {
            AdminLog.add("Unable to setup the session's game. The session is not in a valid state!");
            return CompletableFuture.completedFuture(null);
        }
        var session = sessionRes.get();

        var nextGameId = session.getNext();
        var gameRes = GameRegistry.getGame(nextGameId);
        if (!gameRes.isPresent()) {
            AdminLog.add("Game " + nextGameId + " is not registered, cannot set up!");
            return CompletableFuture.completedFuture(null);
        }
        var game = gameRes.get();

        // pass stuff to the game
        var future = game.setupGame(accessor, session);

        // ensure this runs AFTER the game is made, needs to finalize what the game
        // actually needs in order to be created
        return CompletableFuture.completedFuture(game);
    }
}
