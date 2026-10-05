package com.gaiagauntlet.gauntlet.core.orchestrator.handlers;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.components.PlayerComponent;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.GamePlayerEvent;
import com.gaiagauntlet.gauntlet.core.events.events.GamePlayerEvent.PlayerOp;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.orchestrator.GauntletOrchestrator;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
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
    private static void handlePlayerJoin(GamePlayerEvent evt, GameSession session, PlayerComponent player) {

        var sessionId = evt.getSessionId();
        var playerRef = evt.getPlayer();
        var existing = player.getCurrentSession();
        var party = PartyUtils.getPartyForPlayer(playerRef);
        var partySessionId = PartyUtils.getCurrentSession(party.getId());
        var isOwner = party.getOwner().equals(playerRef.getUuid());
        // can only join if they are the party leader
        // - if party not already in session and not party leader, return
        if (!isOwner) {
            if (!sessionId.equals(partySessionId)) {
                // not owner and owner is not already in session
                Resolve.error(evt, session, msg("server.gg.events.players.join.existing.error")
                        .param("playerName", playerRef.getUsername())
                        .param("partySessionId", partySessionId)
                        .param("sessionId", sessionId)
                        .param("partyName", party.getId()));
                return;
            }
        }

        // set the player (and the party if in a party) to the current session. No going
        // back now
        player.setCurrentSession(sessionId);

        // - leave old session (pulls all party members out of session if party leader)
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

        if (isOwner) {
            // connect all of the party members to this session also
            var members = party.getAllOnlinePlayers();
            for (var member : members) {
                if (member.getUuid().equals(playerRef.getUuid()))
                    continue; // party owner - no need
                try {
                    GauntletEventRegistry.dispatch(new GamePlayerEvent(playerRef, sessionId, PlayerOp.ADD));
                } catch (Exception e) {
                    Resolve.error(evt, session, msg("server.gg.events.players.join.new.error")
                            .param("playerName", playerRef.getUsername()), e);
                    return;
                }
            }
        }

        // actually join the session now
        if (session.available()) {
            GaiaLog.atWarning().withSession(session).log(msg("server.gg.events.players.join.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "session is not active"));
            return;
        }
        var gameId = session.getCurrentGame();
        if (gameId == null || gameId.isEmpty()) {
            GaiaLog.atWarning().withSession(session).log(msg("server.gg.events.players.join.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "no game is active"));
            return;
        }
        if (!(GameRegistry.getGame(gameId).orElse(null) instanceof GameController game)) {
            GaiaLog.atError().withSession(session).log(msg("server.gg.events.players.join.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "game is not registered"));
            return;
        }
        var hubWorld = GauntletUtils.withHubWorld();
        GauntletUtils.run(hubWorld, () -> {
            try {
                // run the player connection logic
                game.playerJoin(hubWorld, partySessionId, playerRef);
            } catch (Exception e) {
                GaiaLog.atError(e).withSession(session).log(msg("server.gg.events.players.join.warn")
                        .param("playerName", playerRef.getUsername())
                        .param("reason", "Exception when handling player connect"));
            }
        });
    }

    // player requested to leave the session / game (you can only do both)
    private static void handlePlayerLeave(GamePlayerEvent evt, GameSession session, PlayerComponent player) {
        var sessionId = evt.getSessionId();
        var playerRef = evt.getPlayer();
        var existing = player.getCurrentSession();
        var party = PartyUtils.getPartyForPlayer(playerRef);
        var partySessionId = PartyUtils.getCurrentSession(party.getId());
        var isOwner = party.getOwner().equals(playerRef.getUuid());
        // can only leave if they are the party leader
        // - if party not already in session and not party leader, return
        if (!isOwner && sessionId.equals(partySessionId)) {
            // not owner and owner is not already in session
            Resolve.error(evt, session, msg("server.gg.events.players.leave.existing.error")
                    .param("playerName", playerRef.getUsername())
                    .param("partySessionId", partySessionId)
                    .param("sessionId", sessionId)
                    .param("partyName", party.getId()));
            return;
        }

        if (existing.equals(sessionId)) {
            // player is in the game being left - set to null
            player.setCurrentSession(null);
        }

        if (isOwner) {
            // connect all of the party members to this session also
            var members = party.getAllOnlinePlayers();
            for (var member : members) {
                if (member.getUuid().equals(playerRef.getUuid()))
                    continue; // party owner - no need
                try {
                    GauntletEventRegistry.dispatch(new GamePlayerEvent(playerRef, sessionId, PlayerOp.REMOVE));
                } catch (Exception e) {
                    Resolve.error(evt, session, msg("server.gg.events.players.leave.new.error")
                            .param("playerName", playerRef.getUsername()), e);
                    return;
                }
            }
        }

        var gameId = session.getCurrentGame();
        if (gameId == null || gameId.isEmpty()) {
            GaiaLog.atWarning().withSession(session).log(msg("server.gg.events.players.leave.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "no game is active"));
            return;
        }
        if (!(GameRegistry.getGame(gameId).orElse(null) instanceof GameController game)) {
            GaiaLog.atError().withSession(session).log(msg("server.gg.events.players.leave.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "game is not registered"));
            return;
        }
        var hubWorld = GauntletUtils.withHubWorld();
        GauntletUtils.run(hubWorld, () -> {
            try {
                // run the player connection logic
                game.playerLeave(hubWorld, partySessionId, playerRef);
            } catch (Exception e) {
                GaiaLog.atError(e).withSession(session).log(msg("server.gg.events.players.leave.warn")
                        .param("playerName", playerRef.getUsername())
                        .param("reason", "Exception when handling player connect"));
            }
        });
    }

    public static void onPlayerReady(PlayerReadyEvent event) {
        var ref = event.getPlayerRef();
        var store = ref.getStore();
        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null)
            return;

        // TODO: Uncomment once huds are merged
        // GauntletOrchestrator.showHud(store, ref, playerRef);
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
        var partySessionId = PartyUtils.getCurrentSession(party.getId());
        var partySession = sessionFor(partySessionId).orElse(null);
        var sessionId = Optional.of(playerComponent.getCurrentSession()).orElse(partySessionId);
        if (sessionId == null || sessionId.isEmpty()) {
            // player and their party not in a session, no-op from here
            GaiaLog.atWarning().log(msg("server.gg.events.players.connect.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "SessionId is not present"));
            return;
        }

        // resolve desired session based on sessionId and partyId
        if (partySession != null && partySessionId.equals(sessionId)) {
            // if player is in a different session from the party
            GaiaLog.atWarning().withSession(partySession).log(msg("server.gg.events.players.connect.wrongsession")
                    .param("playerName", playerRef.getUsername()));
            try {
                // leave old session (they differ - party moved on)
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

        if (!(sessionFor(partySessionId).orElse(null) instanceof GameSession session)) {
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

                // run the player join logic
                game.playerConnect(hubWorld, partySessionId, playerRef);
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
        // TODO: Uncomment once HUDs are merged
        // GauntletOrchestrator.forgetHud(playerRef);

        // leave party
        var party = PartyUtils.getPartyForPlayer(playerRef);
        if (party != null) {

            var future = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                party.clearOffline(playerRef.getUuid());
            }, 180, TimeUnit.SECONDS);

            party.setOffline(playerRef.getUuid(), future);
        }

        var reason = evt.getDisconnectReason();
        GaiaLog.atInfo().log("Player " + playerRef.getUsername() + " disconnected because " + reason);
        var playerComponent = playerRef.getComponentConcurrent(PlayerComponent.getComponentType());
        // safely assume the player component is synced with the party because i'm too
        // lazy to do the party lookup rn
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
