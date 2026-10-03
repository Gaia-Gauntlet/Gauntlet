package com.gaiagauntlet.gauntlet.core.orchestrator.handlers;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.AdminLog;
import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils;
import com.hypixel.hytale.server.core.universe.world.World;

public class SessionHandlers extends HandlerUtils {
    public static void createSession(World hub, GauntletEvent.NewSession sessionEvt) {

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

    public static void handleSession(World hub, GauntletEvent.Session sessionEvt) {
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
            case CANCEL -> {
                cancelGame(hub, sessionEvt, session);
                return;
            }
            case DELETE -> {
                deleteSession(hub, sessionEvt, session);
                return;
            }
        }
    }

    private static void setupGame(World hub, GauntletEvent.Session sessionEvt, GameSession session) {
        if (!session.available()) {
            sessionEvt.Error("Session is in state " + session.getSessionState().toString()
                    + " and is not available for setting up a new game");
            sessionEvt.complete();
            return;
        }

        var nextGameId = session.getNext();
        var gameRes = GameRegistry.getGame(nextGameId);
        if (!gameRes.isPresent()) {
            sessionEvt.Error("No pending game");
            sessionEvt.complete();
            return;
        }
        var game = gameRes.get();

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
                GauntletUtils.run(hub, () -> cancelGame(hub, sessionEvt, session));
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
                GauntletUtils.run(hub, () -> cancelGame(hub, sessionEvt, session));
                return;
            }
            sessionEvt.complete();
        });
    }

    private static void cancelGame(World hub, GauntletEvent.Session sessionEvt, GameSession session) {

        var currentGame = session.getCurrentGame();
        var gameRes = GameRegistry.getGame(currentGame);
        if (!gameRes.isPresent()) {
            sessionEvt.Error("No pending game");
            session.setErrored("No current game is available to cancel");
            sessionEvt.complete();
            return;
        }
        var game = gameRes.get();

        // pass stuff to the game
        var future = game.cleanGame(hub.getEntityStore().getStore(), session);
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

    private static void deleteSession(World hub, GauntletEvent.Session sessionEvt, GameSession session) {
        // if the game is running, cancel it
        if (!session.available()) {
            cancelGame(hub, sessionEvt, session);
            return;
        }

        // clean players
        // for (var player : session.getParticipants())

        // var resource = withResource();
        // resource.removeSession(session);
    }

    public static void sessionQueue(World hub, GauntletEvent.SessionQueue sessionEvt) {
        var sessionOp = sessionFor(sessionEvt.getSessionId());

        if (!sessionOp.isPresent()) {
            sessionEvt.Error("Session " + sessionEvt.getSessionId() + " is not present");
            sessionEvt.complete();
            return;
        }

        var session = sessionOp.get();
        var op = sessionEvt.getOp();
        var gameQueue = sessionEvt.getNewQueue();
        // validate games
        for (var game : gameQueue) {
            if (!GameRegistry.hasGame(game)) {
                // validation failed
                sessionEvt.complete(
                        MessageUtils.error("Game " + game + " is not a valid, registered game! Cancelling operation"));
                return;
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
