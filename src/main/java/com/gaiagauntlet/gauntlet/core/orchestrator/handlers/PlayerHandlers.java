package com.gaiagauntlet.gauntlet.core.orchestrator.handlers;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.error;
import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

import java.util.concurrent.TimeUnit;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.components.PlayerComponent;
import com.gaiagauntlet.gauntlet.core.config.GauntletConfig;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerGameEvent;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerGameEvent.PlayerOp;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerPartyEvent;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.party.components.PartyComponent;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

public class PlayerHandlers extends HandlerUtils {

    public static void handleGamePlayer(World hub, PlayerGameEvent evt) {
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
                    .param("operation", evt.getOperation().toString())
                    .param("reason", "an error was encountered during the handling logic (check logs)")));
        }
    }

    // player requested to join the session / game
    private static void handlePlayerJoin(PlayerGameEvent evt, GameSession session, PlayerComponent player) {

        var sessionId = evt.getSessionId();
        var playerRef = evt.getPlayer();
        var party = PartyUtils.getParty(playerRef);
        var partySession = PartyUtils.sessionFor(party.getId()).orElse(null);
        var partySessionId = partySession != null ? partySession.getId() : null;
        var isOwner = party.getOwner().equals(playerRef.getUuid());
        // can only join if they are the party leader
        // - if party not already in session and not party leader, return
        if (!sessionId.equals(partySessionId)) {
            if (!isOwner) {
                // not owner and owner is not already in session
                Resolve.error(evt, session, msg("server.gg.events.players.join.existing.error")
                        .param("playerName", playerRef.getUsername())
                        .param("partySessionId", partySessionId)
                        .param("sessionId", sessionId)
                        .param("partyName", party.getId()));
                return;
            }

            // otherwise, set them to be the same (as the owner)
            var resource = withResource();
            // auto-removes from old session map
            resource.addPartyToSession(party.getId(), sessionId);
        }

        // - leave old session (pulls all party members out of session if party leader)
        if (partySessionId != null && !partySessionId.equals(sessionId)) {
            // disconnect from prior session first
            // because this is all in the same thread, this will execute and resolve
            // synchronously before the rest of the code here has to finish
            Resolve.log(evt, session, msg("server.gg.events.players.join.existing")
                    .param("playerName", playerRef.getUsername())
                    .param("oldSessionId", partySessionId));
            try {
                GauntletEventRegistry.dispatch(new PlayerGameEvent(playerRef, partySessionId, PlayerOp.REMOVE));
            } catch (Exception e) {
                Resolve.error(evt, session, msg("server.gg.events.players.join.existing.error")
                        .param("playerName", playerRef.getUsername())
                        .param("oldSessionId", partySessionId), e);
                return;
            }
            Resolve.log(evt, session, msg("server.gg.events.players.join.existed")
                    .param("playerName", playerRef.getUsername())
                    .param("oldSessionId", partySessionId));
        }

        if (isOwner) {
            // connect all of the party members to this session also
            var members = party.getAllOnlinePlayers();
            for (var member : members) {
                if (member.getUuid().equals(playerRef.getUuid()))
                    continue; // party owner - no need
                try {
                    GauntletEventRegistry.dispatch(new PlayerGameEvent(member, sessionId, PlayerOp.ADD));
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
        player.setCurrentGame(gameId);
        var hubWorld = GauntletUtils.withHubWorld();
        GauntletUtils.run(hubWorld, () -> {
            try {
                // run the player connection logic
                game.playerJoin(hubWorld, sessionId, List.of(playerRef));
            } catch (Exception e) {
                GaiaLog.atError(e).withSession(session).log(msg("server.gg.events.players.join.warn")
                        .param("playerName", playerRef.getUsername())
                        .param("reason", "Exception when handling player connect"));
            }
        });
    }

    // player requested to leave the session / game (you can only do both)
    private static void handlePlayerLeave(PlayerGameEvent evt, GameSession session, PlayerComponent player) {
        var sessionId = evt.getSessionId();
        var playerRef = evt.getPlayer();
        var party = PartyUtils.getParty(playerRef);
        var partySession = PartyUtils.sessionFor(party.getId()).orElse(null);
        var partySessionId = partySession != null ? partySession.getId() : null;
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

        // remove the party from the session if they match
        if (sessionId.equals(partySessionId)) {
            withResource().removePartyFromSession(party.getId(), sessionId);
        }

        if (isOwner) {
            // connect all of the party members to this session also
            var members = party.getAllOnlinePlayers();
            for (var member : members) {
                if (member.getUuid().equals(playerRef.getUuid()))
                    continue; // party owner - no need
                try {
                    GauntletEventRegistry.dispatch(new PlayerGameEvent(member, sessionId, PlayerOp.REMOVE));
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
                // run the player disconnection logic
                game.playerLeave(hubWorld, sessionId, List.of(playerRef));
                var currentGame = player.getCurrentGame();
                if (currentGame.equals(gameId)) {
                    // only remove as current game once we've confirmed it is still their current
                    // game
                    // this task may be really long, and the player might've changed games during it
                    player.setCurrentGame(null);
                }
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
        var oldGame = playerComponent.getCurrentGame();
        var party = PartyUtils.getParty(playerRef);
        var partySession = PartyUtils.sessionFor(party.getId()).orElse(null);
        var sessionId = partySession != null ? partySession.getId() : null;
        if (sessionId == null || sessionId.isEmpty()) {
            // player and their party not in a session, no-op from here
            GaiaLog.atWarning().log(msg("server.gg.events.players.connect.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "SessionId is not present"));
            return;
        }

        if (!(sessionFor(sessionId).orElse(null) instanceof GameSession session)) {
            GaiaLog.atWarning().log(msg("server.gg.events.players.connect.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "Session not present"));
            return;
        }

        var gameId = session.getCurrentGame();

        var hubWorld = GauntletUtils.withHubWorld();

        // if the player's previous game is not the current game, clean up the previous
        // game's components off the player.
        if (oldGame != null && !oldGame.equals(gameId)) {
            /**
             * I'm not sure this is necessary, and here's why
             * 
             * 1) When the player disconnects, they will run the onDisconnect hook on the
             * controller.
             * 2) The only way for the game to change out from under them is for
             * a- The game ends
             * b- The party moves to another session
             * c- The game crashes
             * Under this assumption, each of those states should clean any session-specific
             * logic up.
             * 
             * Any player-specific logic should be cleaned during the onDisconnect hook.
             * 
             * {@code 
             if (GameRegistry.getGame(oldGame).orElse(null) instanceof GameController game) {
                 GauntletUtils.run(hubWorld, () -> {
                     try {
                         // run the player leave logic
                         GaiaLog.atInfo().withSession(session).log("Player " + playerRef.getUsername() + " being cleaned from old game ("+ oldGame +") before continuing");
                         // cleans the rest of the player, with access to the pl
                         game.playerLeave(hubWorld, null, playerRef);
                     } catch (Exception e) {
                         GaiaLog.atError(e).withSession(session).log(msg("server.gg.events.players.leave.warn")
                                 .param("playerName", playerRef.getUsername())
                                 .param("reason", "Exception when handling player connect"));
                     }
                 });
             }
             }
             * 
             */
        }

        if (session.available()) {
            GaiaLog.atWarning().withSession(session).log(msg("server.gg.events.players.connect.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "session is not active"));
            return;
        }

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

        GauntletUtils.run(hubWorld, () -> {
            try {
                // run the player join logic
                game.playerConnect(hubWorld, sessionId, playerRef);
                playerComponent.setCurrentGame(gameId);
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
        var party = PartyUtils.getParty(playerRef);
        if (party != null) {

            var future = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                party.clearOffline(playerRef.getUuid());
            }, GauntletConfig.get().getTimeoutSeconds(), TimeUnit.SECONDS);

            party.setOffline(playerRef.getUuid(), future);
        }

        var reason = evt.getDisconnectReason();
        GaiaLog.atInfo().log("Player " + playerRef.getUsername() + " disconnected because " + reason);
        var playerComponent = playerRef.getComponentConcurrent(PlayerComponent.getComponentType());
        // safely assume the player component is synced with the party because i'm too
        // lazy to do the party lookup rn
        var session = PartyUtils.sessionFor(party).orElse(null);
        if (session == null) {
            GaiaLog.atWarning().log(msg("server.gg.events.players.disconnect.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "Session not present"));
        }

        var gameId = playerComponent == null ? null : playerComponent.getCurrentGame();
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
                game.playerDisconnect(hubWorld, playerRef);
            } catch (Exception e) {
                GaiaLog.atError(e).withSession(session).log(msg("server.gg.events.players.disconnect.warn")
                        .param("playerName", playerRef.getUsername())
                        .param("reason", "Exception when handling player join"));
            }
        });
    }

    public static void handlePartyPlayer(World hub, PlayerPartyEvent evt) {
        var playerRef = evt.getPlayer();
        var party = PartyUtils.getParty(evt.getPartyId())
                .orElseGet(() -> PartyUtils.getPartyNullable(playerRef).orElse(null));
        if (party == null) {
            // cannot join or leave a party that does not exist
            Resolve.success(evt, "Not in a party and destination party is not valid!");
            return;
        }

        // get the current session of the party (if there is one)
        var session = PartyUtils.sessionFor(party.getId()).orElse(null);

        try {
            switch (evt.getOperation()) {
                case ADD -> {
                    playerJoinParty(evt, party, session);
                }
                case REMOVE -> {
                    playerLeaveParty(evt, party, session);
                }
            }
        } catch (Exception e) {
            evt.complete(GaiaLog.atError(e).withSession(session).log(msg("server.gg.events.players.error")
                    .param("playerName", playerRef.getUsername())
                    .param("sessionId", party.getLabel())
                    .param("operation", evt.getOperation().toString())
                    .param("reason", "an error was encountered during the handling logic (check logs)")));
        }
    }

    public static void playerJoinParty(PlayerPartyEvent evt, PartyComponent newParty, GameSession partySession) {
        var playerRef = evt.getPlayer();

        // leave old party
        PartyUtils.getPartyNullable(playerRef).ifPresent(party -> {
            try {
                GauntletEventRegistry.dispatch(PlayerPartyEvent.Leave(playerRef, party.getId()));
            } catch (Exception e) {
                evt.log(GaiaLog.atWarning().withCause(e).withSession(partySession).log("Failed to leave old party"));
            }
        });

        // add to the new party
        newParty.addPlayer(playerRef.getUuid());

        // at the end, add the player to the session
        if (partySession != null) {
            try {
                GauntletEventRegistry.dispatch(PlayerGameEvent.Add(playerRef, partySession.getId()));
            } catch (Exception e) {
                evt.log(GaiaLog.atWarning().withCause(e).withSession(partySession).log("Failed to join new session"));
            }
        }
        Resolve.success(evt, msg("server.gg.events.players.party.joined").param("player", playerRef.getUsername())
                .param("party", newParty.getLabel()));
    }

    public static void playerLeaveParty(PlayerPartyEvent evt, PartyComponent oldParty, GameSession partySession) {
        var playerRef = evt.getPlayer();
        if (oldParty == null) {
            Resolve.error(evt, error("Unable to leave party. Old party not found!"));
            return;
        }
        var isAlone = oldParty.getAllPlayers().size() == 1;

        // if not alone, leave the party
        if (!isAlone) {
            // leave party - new owner automatically found
            var isOwner = oldParty.getOwner().equals(playerRef.getUuid());
            oldParty.removePlayer(playerRef.getUuid());
            var newOwnerRef = PlayerUtils.get(oldParty.getOwner());
            if (isOwner) {
                PartyUtils.promote(newOwnerRef);
                playerRef.sendMessage(msg("server.gg.commands.party.left.you").param("party", oldParty.getLabel()));
            }
        }

        // leave session
        if (partySession != null) {
            try {
                GauntletEventRegistry.dispatch(PlayerGameEvent.Remove(playerRef, partySession.getId()));
            } catch (Exception e) {
                evt.log(GaiaLog.atWarning().withCause(e).withSession(partySession).log("Failed to leave old session"));
            }
        }
        Resolve.success(evt, msg("server.gg.commands.party.left").param("player", playerRef.getUsername())
                .param("party", oldParty.getLabel()));
    }
}
