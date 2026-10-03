package com.gaiagauntlet.gauntlet.core.orchestrator.handlers;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.AdminLog;
import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.core.events.events.NewSessionEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionQueueEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionQueueEvent.SessionQueueOp;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;

public class SessionHandlers extends HandlerUtils {
    public static void handleNewSession(World hub, NewSessionEvent sessionEvt) {

        var gameSession = sessionEvt.getNewSession();
        // validate loaded games

        if (gameSession.getGameSequence() != null) {

            for (var game : gameSession.getGameSequence()) {
                if (!GameRegistry.hasGame(game)) {
                    sessionEvt.Error("Game " + game + " is not registered!");
                    continue;
                }
                gameSession.addGame(game);
            }
        }

        var success = withResource().addSession(gameSession);
        if (success) {
            sessionEvt
                    .Message(msg("server.gg.commands.session.create.success").param("sessionId", gameSession.getId()));
        } else {
            sessionEvt.Error("Unable to add session! It already exists");
        }
        sessionEvt.complete();
    }

    public static void handleSession(World hub, SessionEvent sessionEvt) {
        var sessionOp = sessionFor(sessionEvt.getSessionId());

        if (!sessionOp.isPresent()) {
            sessionEvt.Error("Session " + sessionEvt.getSessionId() + " is not present");
            sessionEvt.complete();
            return;
        }
        var session = sessionOp.get();

        switch (sessionEvt.getOp()) {
            case SETUP -> {
                setupGame(hub, sessionEvt, session);
                return;
            }
            case CLEAN -> {
                cleanGame(hub, sessionEvt, session);
                return;
            }
            case DELETE -> {
                deleteSession(hub, sessionEvt, session);
                return;
            }
        }
    }

    private static void setupGame(World hub, SessionEvent sessionEvt, GameSession session) {
        if (!session.available()) {
            sessionEvt.Error("Session is in state " + session.getSessionState().toString()
                    + " and is not available for setting up a new game");
            sessionEvt.complete();
            return;
        }

        var nextGameId = session.getNext();

        if (!(GameRegistry.getGame(nextGameId).orElse(null) instanceof GameController game)) {
            sessionEvt.Error("No pending game");
            sessionEvt.complete();
            return;
        }

        // pass stuff to the game
        // mark the next game as running
        session.startNext();
        var future = game.setupGame(hub.getEntityStore().getStore(), session);
        future.whenComplete((value, error) -> {
            if (error != null) {
                // errored
                AdminLog.add("Starting game " + nextGameId + " for session " + session.getId()
                        + " failed to setup with exception: " + error.getLocalizedMessage());
                sessionEvt.Error("Game threw an error during setup! Check logs");

                session.setErrored("Error thrown when setting up");
                // cancel the game immediately - run on the hub thread
                GauntletUtils.run(hub, () -> cleanGame(hub, sessionEvt, session));
                return;
            }

            var check = session.setRunning(nextGameId);
            if (!check) {
                // something has gone horribly wrong
                AdminLog.add("Session " + session.getId() + " in a weird state when starting " + nextGameId
                        + "! Defensively clearing world before things get too bad. Check admin log for details");
                sessionEvt.Error("Game did not setup correctly");

                session.setErrored("Game was in a weird state when starting (session state mismatch)");
                // cancel the game immediately - run on the hub thread
                GauntletUtils.run(hub, () -> cleanGame(hub, sessionEvt, session));
                return;
            }
            sessionEvt.complete();
        });
    }

    private static void cleanGame(World hub, SessionEvent sessionEvt, GameSession session) {
        var currentGame = session.getCurrentGame();
        var isCleaning = session.setCleaning(currentGame);

        if (!isCleaning) {
            sessionEvt.complete(Message.raw("Already cleaning!"));
            return;
        }

        if (!(GameRegistry.getGame(currentGame).orElse(null) instanceof GameController game)) {
            sessionEvt.Error("No pending game");
            session.setErrored("No current game is available to cancel");
            sessionEvt.complete();
            return;
        }

        // pass stuff to the game
        var future = game.cleanGame(hub, session);
        future.whenComplete((value, error) -> {
            if (error != null) {
                AdminLog.add("Cancelling game " + currentGame + " for session " + session.getId()
                        + " failed! Error: " + error.getLocalizedMessage());
                sessionEvt.Error("Game threw an error while being cancelled! Check logs");
                session.setErrored("Error thrown when cancelling");
            } else {
                session.setComplete(currentGame);
            }
            sessionEvt.complete();
        });
    }

    private static void deleteSession(World hub, SessionEvent sessionEvt, GameSession session) {
        // if the game is running, cancel it
        if (!session.available()) {
            cleanGame(hub, sessionEvt, session);
            return;
        }

        // clean players
        // for (var player : session.getParticipants())

        var resource = withResource();
        resource.removeSession(session);
        sessionEvt.complete(Message.raw("Destroyed " + session.getId()));
    }

    public static void handleSessionQueue(World hub, SessionQueueEvent sessionEvt) {
        if (!(sessionFor(sessionEvt.getSessionId()).orElse(null) instanceof GameSession session)) {
            sessionEvt.Error("Session " + sessionEvt.getSessionId() + " is not present");
            sessionEvt.complete();
            return;
        }

        var op = sessionEvt.getOp();
        var gameQueue = sessionEvt.getNewQueue();
        // validate games
        if (op != SessionQueueOp.REMOVE) {

            for (var game : gameQueue) {
                if (!GameRegistry.hasGame(game)) {
                    // validation failed
                    sessionEvt.complete(
                            MessageUtils
                                    .error("Game " + game + " is not a valid, registered game! Cancelling operation"));
                    return;
                }
            }
        }

        switch (op) {
            case SET -> {
                session.setGames(gameQueue);
                sessionEvt.complete();
                return;
            }
            case REMOVE -> {
                for (var game : gameQueue) {
                    session.removeGame(game);
                }
                sessionEvt.complete();
                return;
            }
            case APPEND -> {
                session.addGames(gameQueue);
                sessionEvt.complete();
            }
        }
    }
}
