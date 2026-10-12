package com.gaiagauntlet.gauntlet.core.orchestrator.handlers;

import java.util.function.Consumer;
import com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;
import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.error;
import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

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
import com.gaiagauntlet.gauntlet.core.orchestrator.GauntletOrchestrator;
import com.gaiagauntlet.gauntlet.core.party.components.PartyComponent;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.session.constants.SessionState;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.server.core.Message;
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

        var players = new HashSet<>(GauntletUtils.playersFor(session));
        players.add(playerRef);
        refreshHud(players);
    }

    // player requested to join the session / game
    private static void handlePlayerJoin(PlayerGameEvent evt, GameSession session, PlayerComponent player) {

        var sessionId = evt.getSessionId();
        var playerRef = evt.getPlayer();
        var slot = withResource().takeOffline(playerRef.getUuid());
        if (slot != null && slot.sessionId() != null && !slot.sessionId().equals(sessionId))
            sessionFor(slot.sessionId()).ifPresent(old -> leaveGame(old, old.getCurrentGame(), playerRef, true));
        var party = PartyUtils.ensureParty(playerRef);
        var partySession = PartyUtils.sessionFor(party.getId()).orElse(null);
        var partySessionId = partySession != null ? partySession.getId() : null;
        var isOwner = party.getOwner().equals(playerRef.getUuid());
        // can only join if they are the party leader
        // - if party not already in session and not party leader, return
        if (!sessionId.equals(partySessionId)) {
            if (!isOwner) {
                // not owner and owner is not already in session
                Resolve.error(evt, session, msg("server.gg.events.players.join.existing.notowner")
                        .param("playerName", playerRef.getUsername())
                        .param("partySessionId", Objects.requireNonNullElse(partySessionId, "none"))
                        .param("sessionId", sessionId)
                        .param("partyName", party.getId()));
                return;
            }

            // if partyId is not null, then leave the session
            if (partySessionId != null) {
                Resolve.log(evt, session, msg("server.gg.events.players.join.existing")
                        .param("playerName", playerRef.getUsername())
                        .param("oldSessionId", partySessionId));
                var leave = new PlayerGameEvent(playerRef, partySessionId, PlayerOp.REMOVE);
                leave.onComplete(_ -> GauntletUtils.run(GauntletUtils.withHubWorld(), () -> {
                    withResource().addPartyToSession(party.getId(), sessionId);
                    enterSession(evt, session, playerRef, party);
                }));
                try {
                    GauntletEventRegistry.dispatch(leave);
                } catch (Exception e) {
                    Resolve.error(evt, session, msg("server.gg.events.players.join.existing.error")
                            .param("playerName", playerRef.getUsername())
                            .param("oldSessionId", partySessionId), e);
                }
                return;
            }
            withResource().addPartyToSession(party.getId(), sessionId);
        }
        enterSession(evt, session, playerRef, party);
    }

    private static void enterSession(PlayerGameEvent evt, GameSession session, PlayerRef playerRef, PartyComponent party) {
        var sessionId = session.getId();
        if (party.getOwner().equals(playerRef.getUuid())) {
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
        if (session.getSessionState() != SessionState.RUNNING) {
            evt.complete(GaiaLog.atInfo().withSession(session).log(msg("server.gg.events.players.join.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "the session is " + session.getSessionState()
                            + ", they will be sent in once its game is running")));
            return;
        }

        var hubWorld = GauntletUtils.withHubWorld();

        // join the game
        GauntletUtils.run(hubWorld, () -> {
            try {
                // run the player connection logic
                joinGame(hubWorld, session, List.of(playerRef)).whenComplete((_, error) -> {
                    if (error != null) {
                        Resolve.error(evt, session, msg("server.gg.events.players.join.new.error")
                                .param("playerName", playerRef.getUsername()), error);
                        return;
                    }
                    Resolve.success(evt, session, msg("server.gg.events.players.join.success")
                            .param("playerName", playerRef.getUsername()));
                    return;
                });
            } catch (Exception e) {
                evt.complete(GaiaLog.atError(e).withSession(session).log(msg("server.gg.events.players.join.warn")
                        .param("playerName", playerRef.getUsername())
                        .param("reason", MessageUtils.reason(e))));
            }
        });
    }

    // player requested to leave the session / game (you can only do both)
    private static void handlePlayerLeave(PlayerGameEvent evt, GameSession session, PlayerComponent player) {
        var sessionId = evt.getSessionId();
        var playerRef = evt.getPlayer();
        withResource().takeOffline(playerRef.getUuid());
        var party = PartyUtils.getParty(playerRef).orElse(null);
        if (party == null) {
            Resolve.error(evt, session, msg("server.gg.events.players.leave.new.error")
                    .param("playerName", playerRef.getUsername())
                    .param("sessionId", sessionId)
                    .param("cause", "Player is not in a party!"));
            return;
        }
        var partySession = PartyUtils.sessionFor(party.getId()).orElse(null);
        var partySessionId = partySession != null ? partySession.getId() : null;
        var isOwner = party.getOwner().equals(playerRef.getUuid());
        // can only leave if they are the party leader
        // - if party not already in session and not party leader, return
        if (!isOwner && sessionId.equals(partySessionId)) {
            // not owner and owner is not already in session
            Resolve.error(evt, session, msg("server.gg.events.players.leave.existing.error")
                    .param("playerName", playerRef.getUsername())
                    .param("partySessionId", Objects.requireNonNullElse(partySessionId, "none"))
                    .param("sessionId", sessionId)
                    .param("partyName", party.getId()));
            return;

        }

        // remove the party from the session if they match
        if (sessionId.equals(partySessionId)) {
            withResource().removePartyFromSession(party.getId(), sessionId);
        }

        if (isOwner && sessionId.equals(partySessionId)) {
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
            evt.complete(GaiaLog.atWarning().withSession(session).log(msg("server.gg.events.players.leave.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "no game is active")));
            return;
        }
        if (!(GameRegistry.getGame(gameId).orElse(null) instanceof GameController game)) {
            evt.complete(GaiaLog.atError().withSession(session).log(msg("server.gg.events.players.leave.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "game is not registered")));
            return;
        }
        var hubWorld = GauntletUtils.withHubWorld();
        GauntletUtils.run(hubWorld, () -> {
            try {
                var currentGame = player.getCurrentGame();
                if (currentGame != null && currentGame.equals(gameId)) {
                    // only remove as current game once we've confirmed it is still their current
                    // game
                    // this task may be really long, and the player might've changed games during it
                    player.setCurrentGame(null);
                }
                // run the player disconnection logic
                game.playerLeave(hubWorld, sessionId, List.of(playerRef), true).whenComplete((_, error) -> {
                    if (error != null) {
                        Resolve.error(evt, session, msg("server.gg.events.players.leave.new.error")
                                .param("playerName", playerRef.getUsername()), error);
                        return;
                    }

                    Resolve.success(evt, session, msg("server.gg.events.players.leave.success")
                            .param("playerName", playerRef.getUsername()));

                    return;
                });
            } catch (Exception e) {
                evt.complete(GaiaLog.atError(e).withSession(session).log(msg("server.gg.events.players.leave.warn")
                        .param("playerName", playerRef.getUsername())
                        .param("reason", "the game controller threw " + e.getClass().getSimpleName())));
            }
        });
    }

    public static void onPlayerReady(PlayerReadyEvent event) {
        var ref = event.getPlayerRef();
        var store = ref.getStore();
        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null)
            return;

        GauntletOrchestrator.showHud(store, ref, playerRef);
        sessionFor(playerRef).ifPresent(HandlerUtils::refreshHud);
    }

    /** Puts a reconnecting player back into the session they dropped out of, if their slot is still held. */
    public static boolean rejoin(PlayerRef playerRef, Consumer<Message> reply) {
        var resource = withResource();
        var slot = resource.takeOffline(playerRef.getUuid());
        if (slot == null)
            return false;
        var session = slot.sessionId() == null ? null : sessionFor(slot.sessionId()).orElse(null);
        var party = PartyUtils.getParty(slot.partyId()).orElse(null);
        var partyKept = party != null
                && Objects.equals(PartyUtils.sessionFor(party).map(GameSession::getId).orElse(null), slot.sessionId());
        if (partyKept) {
            reply.accept(msg("server.gg.gauntlet.party.rejoining").param("party", party.getLabel()));
            GauntletEventRegistry.dispatch(PlayerPartyEvent.Join(playerRef, party.getId())
                    .onComplete(log -> reply.accept(log.toMessage())));
            return true;
        }
        var current = PartyUtils.getParty(playerRef).orElse(null);
        if (session == null || (current != null && PartyUtils.sessionFor(current).isPresent()))
            return false;
        var solo = PartyUtils.ensureParty(playerRef);
        resource.addPartyToSession(solo.getId(), session.getId());
        reply.accept(msg("server.gg.gauntlet.party.rejoining").param("party", solo.getLabel()));
        GauntletEventRegistry.dispatch(PlayerGameEvent.Add(playerRef, session.getId())
                .onComplete(log -> reply.accept(log.toMessage())));
        return true;
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
        // just ensure the player has the playerComponent for now - there may be more
        // setup to do later
        holder.ensureAndGetComponent(PlayerComponent.getComponentType());

        var slot = withResource().getOffline(playerRef.getUuid());
        if (slot != null) {
            var party = PartyUtils.getParty(slot.partyId()).map(PartyComponent::getLabel).orElse("your match");
            playerRef.sendMessage(msg("server.gg.gauntlet.party.rejoin")
                    .param("party", party)
                    .param("duration", slot.secondsLeft()));
        }

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
        GauntletOrchestrator.forgetHud(playerRef);
        var party = PartyUtils.getParty(playerRef).orElse(null);

        var playerComponent = playerRef.getComponentConcurrent(PlayerComponent.getComponentType());

        var session = PartyUtils.sessionFor(party).orElse(null);

        var reason = evt.getDisconnectReason();
        var reasonText = reason == null ? "unknown"
                : reason.getServerDisconnectReason() != null ? "server: " + reason.getServerDisconnectReason()
                        : "client: " + reason.getClientDisconnectType();
        GaiaLog.atInfo().withSession(session)
                .log("Player " + playerRef.getUsername() + " disconnected (" + reasonText + ")");

        // player state is read before the hop since the engine may finish removing the
        // player first
        GauntletUtils.run(GauntletUtils.withHubWorld(), () -> {
            if (party != null) {
                if (!party.includesPlayer(playerRef))
                    return;
                party.removePlayer(playerRef.getUuid());
                withResource().markOffline(playerRef.getUuid(), session == null ? null : session.getId(), party.getId(),
                        GauntletConfig.get().getTimeoutSeconds());
            }

            if (session == null) {
                GaiaLog.atWarning().log(msg("server.gg.events.players.disconnect.warn")
                        .param("playerName", playerRef.getUsername())
                        .param("reason", "Session not present"));
                return;
            }
            var gameId = playerComponent == null ? null : playerComponent.getCurrentGame();
            leaveGame(session, gameId, playerRef, false);
        });
    }

    private static void leaveGame(GameSession session, @Nullable String gameId, PlayerRef playerRef, boolean forfeit) {
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
        try {
            game.playerLeave(GauntletUtils.withHubWorld(), session.getId(), List.of(playerRef), forfeit);
        } catch (Exception e) {
            GaiaLog.atError(e).withSession(session).log(msg("server.gg.events.players.disconnect.warn")
                    .param("playerName", playerRef.getUsername())
                    .param("reason", "the game controller threw " + e.getClass().getSimpleName()));
        }
    }

    public static void handlePartyPlayer(World hub, PlayerPartyEvent evt) {
        var playerRef = evt.getPlayer();

        if (playerRef == null) {
            Resolve.error(evt, error("Cannot perform action, PlayerRef is null"));
            return;
        }
        var party = PartyUtils.getParty(evt.getPartyId())
                .orElseGet(() -> PartyUtils.getParty(playerRef).orElse(null));
        if (party == null) {
            // cannot join or leave a party that does not exist
            Resolve.success(evt,
                    playerRef.getUsername() + " is not in a party and party " + evt.getPartyId() + " does not exist");
            return;
        }

        // get the current session of the party (if there is one)
        var session = PartyUtils.sessionFor(party.getId()).orElse(null);

        try {
            switch (evt.getOperation()) {
                case ADD -> {
                    playerJoinParty(evt, playerRef, party, session);
                }
                case REMOVE -> {
                    playerLeaveParty(evt, playerRef, party, session);
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

    public static void playerJoinParty(PlayerPartyEvent evt, PlayerRef playerRef, PartyComponent newParty,
            GameSession partySession) {
        var oldParty = PartyUtils.getParty(playerRef).filter(party -> !party.getId().equals(newParty.getId()))
                .orElse(null);
        var oldSession = PartyUtils.sessionFor(oldParty).orElse(null);

        newParty.addPlayer(playerRef.getUuid());

        if (oldParty != null) {
            try {
                // runs synchronously since both events happen in the same hub thread
                GauntletEventRegistry.dispatch(PlayerPartyEvent.Leave(playerRef, oldParty.getId()));

                // if the player is still in the party and the party is one - party of one issue
                // and should be cleaned
                if (oldParty.includesPlayer(playerRef) && oldParty.size() == 1) {
                    withResource().removeParty(oldParty.getId());
                }
            } catch (Exception e) {
                evt.log(GaiaLog.atWarning().withCause(e).withSession(partySession).log("Failed to leave old party"));
            }
        }

        var oldSessionId = oldSession == null ? null : oldSession.getId();
        var newSessionId = partySession == null ? null : partySession.getId();
        if (Objects.equals(oldSessionId, newSessionId)) {
            if (partySession != null)
                refreshHud(partySession);
        } else if (oldSessionId == null) {
            joinPartySession(evt, playerRef, partySession);
        } else {
            var leave = PlayerGameEvent.Remove(playerRef, oldSessionId);
            leave.onComplete(_ -> GauntletUtils.run(GauntletUtils.withHubWorld(),
                    () -> joinPartySession(evt, playerRef, partySession)));
            try {
                GauntletEventRegistry.dispatch(leave);
            } catch (Exception e) {
                evt.log(GaiaLog.atWarning().withCause(e).withSession(oldSession).log("Failed to leave old session"));
            }
        }
        Resolve.success(evt, msg("server.gg.events.players.party.joined").param("player", playerRef.getUsername())
                .param("party", newParty.getLabel()));
    }

    private static void joinPartySession(PlayerPartyEvent evt, PlayerRef playerRef, @Nullable GameSession partySession) {
        if (partySession == null)
            return;
        try {
            GauntletEventRegistry.dispatch(PlayerGameEvent.Add(playerRef, partySession.getId()));
        } catch (Exception e) {
            evt.log(GaiaLog.atWarning().withCause(e).withSession(partySession).log("Failed to join new session"));
        }
    }

    private static void playerLeaveParty(PlayerPartyEvent evt, PlayerRef playerRef, PartyComponent oldParty,
            GameSession partySession) {
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

        // a player moving to another party has their session handled by playerJoinParty, anyone else keeps
        // playing in a party of their own
        var switching = withResource().getParties().values().stream()
                .anyMatch(party -> party != oldParty && party.includesPlayer(playerRef));
        if (partySession != null && !switching && !isAlone) {
            var soloParty = PartyUtils.ensureParty(playerRef);
            withResource().addPartyToSession(soloParty.getId(), partySession.getId());
            refreshHud(partySession);
        }
        Resolve.success(evt, msg("server.gg.commands.party.left").param("player", playerRef.getUsername())
                .param("party", oldParty.getLabel()));
    }
}
