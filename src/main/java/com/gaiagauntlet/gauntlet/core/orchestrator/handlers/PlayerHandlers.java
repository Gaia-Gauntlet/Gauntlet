package com.gaiagauntlet.gauntlet.core.orchestrator.handlers;

import java.util.Optional;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.components.PlayerComponent;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.GamePlayerEvent;
import com.gaiagauntlet.gauntlet.core.events.events.GamePlayerEvent.PlayerOp;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.world.World;

public class PlayerHandlers extends HandlerUtils {

    public static void handleGamePlayer(World hub, GamePlayerEvent evt) {
        var sessionId = evt.getSessionId();
        var playerRef = evt.getPlayer();
        if (!(sessionFor(sessionId).orElse(null) instanceof GameSession session)) {
            Resolve.error(evt, msg("server.gg.events.players.error")
                    .param("playerName", playerRef.getUsername())
                    .param("sessionId", sessionId)
                    .param("operation", evt.getOperation().toString())
                    .param("reason", "Session not registered"));
            return;
        }

        if (!(playerFor(playerRef).orElse(null) instanceof PlayerComponent playerComp)) {
            Resolve.error(evt, session, msg("server.gg.events.players.error")
                    .param("playerName", playerRef.getUsername())
                    .param("operation", evt.getOperation().toString())
                    .param("reason", "PlayerComponent is null or not found"));
            return;
        }

        try {
            switch (evt.getOperation()) {
                case ADD -> {
                    handlePlayerJoin(evt, session, playerComp);
                }
                case REMOVE -> {
                    handlePlayerLeave(evt, session, playerComp);
                }
            }
        } catch (Exception e) {
            evt.complete(GaiaLog.atError(e).withSession(session).log(msg("server.gg.events.players.error")
                    .param("playerName", playerRef.getUsername())
                    .param("sessionId", sessionId)
                    .param("reason", "an error was encountered during the handling logic (check logs)")));
        }
    }

    // player requested to join the session / game
    private static void handlePlayerJoin(GamePlayerEvent evt, GameSession session, PlayerComponent playerComp) {
        var sessionId = evt.getSessionId();
        var playerRef = evt.getPlayer();
        var existing = playerComp.getCurrentSession();
        var party = PartyUtils.getPartyForPlayer(playerRef);
        var partySessionId = party.getSession();
        // can only join if they are the party leader
        if (!playerRef.getUuid().equals(party.getOwner())) {
            // player is not the owner - cannot join the session. Check if the session is
            // new
            if (!sessionId.equals(partySessionId)) {
                // attempting to go to a new session, prevent action
                Resolve.error(evt, session, msg("server.gg.events.players.join.existing.error")
                        .param("playerName", playerRef.getUsername())
                        .param("partySessionId", partySessionId)
                        .param("sessionId", sessionId)
                        .param("partyName", party.getId()));
                return;
            }
        }

        // joining a session you are already in is fine and should be allowed in case
        // stuff goes wrong
        if (existing != null && !existing.equals(sessionId)) {
            // disconnect from prior session first
            // because this is all in the same thread, this will execute and resolve
            // synchronously before the rest of the code here has to finish
            Resolve.log(evt, session, msg("server.gg.events.players.join.existing")
                    .param("playerName", playerRef.getUsername())
                    .param("oldSessionId", existing));
            try {
                GauntletEventRegistry.dispatch(new GamePlayerEvent(playerRef, existing, PlayerOp.REMOVE));
            } catch (Exception e) {
                Resolve.error(evt, session, msg("server.gg.events.players.join.existing.error")
                        .param("playerName", playerRef.getUsername())
                        .param("oldSessionId", existing), e);
                return;
            }
            Resolve.log(evt, session, msg("server.gg.events.players.join.existed")
                    .param("playerName", playerRef.getUsername())
                    .param("oldSessionId", existing));
        }

        // now to add the player to the new session

        // add the player to the game
    }

    // player requested to leave the session / game (you can only do both)
    private static void handlePlayerLeave(GamePlayerEvent evt, GameSession session, PlayerComponent playerComp) {

    }

    /**
     * 1. Setup Player (add components?)<br />
     * 2. Route player to their game controller (if session is active)<br />
     * 3. Hand off the rest of player connection to the controller
     * 
     * @param evt
     */
    public static void onPlayerConnect(PlayerConnectEvent evt) {
        var playerRef = evt.getPlayerRef();
        if (playerRef == null) { // defensive null-check on the playerRef because hytale do be funky sometimes
            // player not in a session, no-op from here
            GaiaLog.atWarning().log(msg("server.gg.events.players.connect.warn")
                    .param("playerName", "Unknown")
                    .param("reason", "Player's reference is not present"));
            return;
        }
        var holder = evt.getHolder();
        var playerComponent = holder.ensureAndGetComponent(PlayerComponent.getComponentType());
        var party = PartyUtils.getPartyForPlayer(playerRef);
        var partySessionId = party.getSession();
        var partySession = sessionFor(partySessionId).orElse(null);
        var sessionId = Optional.of(playerComponent.getCurrentSession()).orElse(partySessionId);
        if (sessionId == null || sessionId.isEmpty()) {
            // player and their party not in a session, no-op from here
            GaiaLog.atWarning().log(msg("server.gg.events.players.connect.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "SessionId is not present"));
            return;
        }

        if (partySession != null && partySessionId.equals(sessionId)) {
            // if player is in a different session from the party
            GaiaLog.atWarning().withSession(partySession).log(msg("server.gg.events.players.connect.wrongsession")
                    .param("playerName", playerRef.getUsername()));
            try {
                GauntletEventRegistry.dispatch(new GamePlayerEvent(playerRef, sessionId, PlayerOp.REMOVE));
            } catch (Exception e) {
                GaiaLog.atError(e).withSession(partySession).log(msg("server.gg.events.players.join.existing.error")
                        .param("playerName", playerRef.getUsername())
                        .param("sessionId", partySessionId)
                        .param("oldSessionId", sessionId));
                return;
            }
            GaiaLog.atWarning().withSession(partySession).log(msg("server.gg.events.players.join.existed")
                    .param("playerName", playerRef.getUsername())
                    .param("sessionId", partySessionId)
                    .param("oldSessionId", sessionId));
        }

        if (!(sessionFor(sessionId).orElse(null) instanceof GameSession session)) {
            GaiaLog.atWarning().log(msg("server.gg.events.players.connect.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "Session not present"));
            return;
        }

        if (session.available()) {
            GaiaLog.atWarning().withSession(session).log(msg("server.gg.events.players.connect.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "session is not active"));
            return;
        }

        var gameId = session.getCurrentGame();
        if (gameId == null || gameId.isEmpty()) {
            GaiaLog.atWarning().withSession(session).log(msg("server.gg.events.players.connect.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "no game is active"));
            return;
        }
        if (!(GameRegistry.getGame(gameId).orElse(null) instanceof GameController game)) {
            GaiaLog.atError().withSession(session).log(msg("server.gg.events.players.connect.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "game is not registered"));
            return;
        }
        var hubWorld = GauntletUtils.withHubWorld();
        GauntletUtils.run(hubWorld, () -> {
            try {
                game.playerConnect(hubWorld, sessionId, playerRef);
            } catch (Exception e) {
                GaiaLog.atError(e).withSession(session).log(msg("server.gg.events.players.connect.warn")
                        .param("playerName", playerRef.getUsername())
                        .param("reason", "Exception when handling player join"));
            }
        });
    }

    /**
     * 1. Not sure there is anything to actually do here?<br />
     * 2. Inform any active controller that the player has disconnected
     * 
     * @param evt
     */
    public static void onPlayerDisconnect(PlayerDisconnectEvent evt) {
        var playerRef = evt.getPlayerRef();
        if (playerRef == null) { // defensive null-check on the playerRef because hytale do be funky sometimes
            // player not in a session, no-op from here
            GaiaLog.atWarning().log(msg("server.gg.events.players.disconnect.warn")
                    .param("playerName", "Unknown")
                    .param("reason", "Player's reference is not present"));
            return;
        }
        var reason = evt.getDisconnectReason();
        GaiaLog.atInfo().log("Player " + playerRef.getUsername() + " disconnected because " + reason);
        var playerComponent = playerRef.getComponentConcurrent(PlayerComponent.getComponentType());
        // safely assume the player component is synced with the party because i'm too lazy to do the party lookup rn
        var sessionId = playerComponent.getCurrentSession();
        if (sessionId == null || sessionId.isEmpty()) {
            // player not in a session, no-op from here
            GaiaLog.atWarning().log(msg("server.gg.events.players.disconnect.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "SessionId is not present"));
            return;
        }
        if (!(sessionFor(sessionId).orElse(null) instanceof GameSession session)) {
            GaiaLog.atWarning().log(msg("server.gg.events.players.disconnect.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "Session not present"));
            return;
        }

        if (session.available()) {
            GaiaLog.atWarning().withSession(session).log(msg("server.gg.events.players.disconnect.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "session is not active"));
            return;
        }

        var gameId = session.getCurrentGame();
        if (gameId == null || gameId.isEmpty()) {
            GaiaLog.atWarning().withSession(session).log(msg("server.gg.events.players.disconnect.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "no game is active"));
            return;
        }

        if (!(GameRegistry.getGame(gameId).orElse(null) instanceof GameController game)) {
            GaiaLog.atError().withSession(session).log(msg("server.gg.events.players.disconnect.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "game is not registered"));
            return;
        }

        var hubWorld = GauntletUtils.withHubWorld();
        GauntletUtils.run(hubWorld, () -> {
            try {
                game.playerDisconnect(hubWorld, sessionId, playerRef);
            } catch (Exception e) {
                GaiaLog.atError(e).withSession(session).log(msg("server.gg.events.players.disconnect.warn")
                        .param("playerName", playerRef.getUsername())
                        .param("reason", "Exception when handling player join"));
            }
        });
    }
}
