package com.gaiagauntlet.gauntlet.core.orchestrator.handlers;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.components.PlayerComponent;
import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.core.events.events.GameEvent;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.resources.UniverseGauntletResource;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.session.constants.SessionState;
import com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

/**
 * Bunch of utilities for the handlers so I don't have to repeat myself a
 * thousand times
 */
public class HandlerUtils {
    public static UniverseGauntletResource withResource() {
        return GauntletUtils.withResource();
    }

    @Nonnull
    public static Optional<GameSession> sessionFor(@Nonnull String id) {
        return GauntletUtils.sessionFor(id);
    }

    @Nonnull
    public static Optional<GameSession> sessionFor(@Nonnull PlayerRef player) {
        return GauntletUtils.sessionFor(player);
    }

    @Nonnull
    public static Optional<PlayerComponent> playerFor(@Nonnull PlayerRef player) {
        return GauntletUtils.playerFor(player);
    }

    /**
     * Adds the player list to a game
     * @throws IllegalArgumentException when the game encounters an error during completion
     * @param hub
     * @param session
     * @param players
     * @return
     */
    public static CompletableFuture<Void> joinGame(World hub, GameSession session, Collection<PlayerRef> players) {
        if (players.isEmpty() || session.getSessionState() != SessionState.RUNNING) {
            throw new IllegalArgumentException("Game state is " + session.getSessionState().toString());
        }

        var gameId = session.getCurrentGame();

        if (gameId == null || gameId.isEmpty()) {
            throw new IllegalArgumentException("No game is active");
        }

        if (!(GameRegistry.getGame(gameId).orElse(null) instanceof GameController game)) {
            throw new IllegalArgumentException("No game is not registered");
        }

        for (var player : players) {
            var playerComp = GauntletUtils.playerFor(player);
            playerComp.ifPresent(comp -> {
                comp.setCurrentGame(gameId);
            });
        }

        // add all of the players to the game
        return game.playerJoin(hub, gameId, players);
    }

    public class Resolve {
        public static void error(GauntletEvent evt, GameSession session, Message mes, Throwable e) {
            evt.complete(
                    GaiaLog.atError(e).withSession(session)
                            .log(mes
                                    .param("sessionId", session.getId())
                                    .param("cause", e.getLocalizedMessage())
                                    .param("gameId", session.getCurrentGame())));
        }

        public static void error(GauntletEvent evt, GameSession session, Message mes) {
            error(evt, session, mes, new IllegalStateException("Invalid State"));
        }

        public static void error(GauntletEvent evt, Message mes) {
            evt.complete(
                    GaiaLog.atError()
                            .log(mes));
        }

        public static void error(GauntletEvent evt, GameSession session, String key) {
            error(evt, session, MessageUtils.msg(key));
        }

        public static void success(GauntletEvent evt, String text) {
            evt.complete(
                    GaiaLog.atInfo()
                            .log(text));
        }

        public static void success(GauntletEvent evt, Message mes) {
            evt.complete(
                    GaiaLog.atInfo()
                            .log(mes));
        }

        public static void success(GauntletEvent evt, GameSession session, Message mes) {
            evt.complete(
                    GaiaLog.atInfo().withSession(session)
                            .log(mes.param("sessionId", session.getId()).param("gameId", session.getCurrentGame())));
        }

        public static void log(GauntletEvent evt, GameSession session, Message mes) {
            evt.log(
                    GaiaLog.atInfo().withSession(session)
                            .log(mes.param("sessionId", session.getId()).param("gameId", session.getCurrentGame())));
        }

        public static void log(GauntletEvent evt, GameSession session, String key) {
            log(evt, session, MessageUtils.msg(key));
        }

        public static void success(GauntletEvent evt, GameSession session, String key) {
            success(evt, session, MessageUtils.msg(key));
        }
    }
}
