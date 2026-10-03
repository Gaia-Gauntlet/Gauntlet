package com.gaiagauntlet.gauntlet.core.orchestrator.handlers;

import java.util.concurrent.TimeUnit;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.GameEndEvent;
import com.gaiagauntlet.gauntlet.core.events.events.GameEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionEvent.SessionOperation;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.session.constants.SessionState;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;

public class GameHandlers extends HandlerUtils {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void handleGame(World hub, GameEvent sessionEvt) {
        throw new IllegalAccessError("Not implemented!");
    }

    public static void handleGameEnd(World hub, GameEndEvent sessionEvt) {

        var sessionId = sessionEvt.getSessionId();
        if (!(sessionFor(sessionId).orElse(null) instanceof GameSession session)) {
            sessionEvt.Error("Session " + sessionEvt.getSessionId() + " is not present");
            sessionEvt.complete();
            return;
        }

        if (session.getSessionState() != SessionState.RUNNING) {
            sessionEvt.complete(Message.raw("Game already ended"));
            return;
        }

        var gameId = sessionEvt.getGameId();

        // wait for the timers to be done
        sessionEvt.settled().orTimeout(5, TimeUnit.SECONDS).whenComplete((a, error) -> {
            // log error, continue anyways
            if (error != null) {
                LOGGER.atSevere().withCause(error).log("Failed to finalize deferred tasks for %s when closing %s",
                        sessionId, gameId);
                sessionEvt.Error("Game failed to ");
                // continue with cleaning regardless
            }

            // clean the game
            GauntletEventRegistry.dispatch(new SessionEvent(SessionOperation.CLEAN, sessionId));

        });
    }
}
