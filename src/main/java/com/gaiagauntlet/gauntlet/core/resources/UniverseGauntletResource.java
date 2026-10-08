package com.gaiagauntlet.gauntlet.core.resources;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import com.gaiagauntlet.gauntlet.core.party.components.PartyComponent;
import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.resources.UniverseResourceType;

import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Universe-scoped resource for game management. Mutations should only happen
 * via the Orchestrator and nowhere else.
 */
public class UniverseGauntletResource {
    public static final String ID = "UniverseGauntletResource";
    public static final BuilderCodec<@NotNull UniverseGauntletResource> CODEC = BuilderCodec
            .builder(UniverseGauntletResource.class, UniverseGauntletResource::new)
            .append(new KeyedCodec<>("Sessions",
                    new MapCodec<>(GameSession.CODEC, ConcurrentHashMap::new, false)),
                    (resource, v) -> resource.sessions = v,
                    resource -> resource.sessions)
            .add()
            .append(new KeyedCodec<>("Parties",
                    new MapCodec<>(PartyComponent.CODEC, ConcurrentHashMap::new, false)),
                    (resource, v) -> resource.parties = v,
                    resource -> resource.parties)
            .add()
            .build();

    @Setter @Getter private static UniverseResourceType<UniverseGauntletResource> resourceType;
    @Nonnull @Getter private Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    @Nonnull @Getter private Map<String, PartyComponent> parties = new ConcurrentHashMap<>();

    private Map<UUID, Pair<ScheduledFuture<?>, String>> offlinePlayers = new ConcurrentHashMap<>();

    // faster lookup maps for hotpath efficiency. Should not be considered the
    // source of truth
    private Map<String, String> partyIdToGameSession = new ConcurrentHashMap<>();
    private Map<UUID, String> playerToPartyId = new ConcurrentHashMap<>();

    // Sessions

    public Optional<GameSession> getSession(String id) {
        return Optional.ofNullable(sessions.get(id));
    }

    public boolean addSession(GameSession session) {
        if (sessions.containsKey(session.getId())) {
            return false; // unable to add duplicate
        }

        sessions.put(session.getId(), session);
        return true;
    }

    /**
     * Deletes the session. Should be run from the Orchestrator's delete session.
     * This does zero clean-up and may lead to stale/missing/broken data
     */
    public boolean removeSession(GameSession session) {
        if (session == null || session.getId() == null)
            return false;
        if (!sessions.containsKey(session.getId())) {
            return false; // to remove
        }
        sessions.remove(session.getId());
        // clean the lookup map
        for (var partyId : session.getParties()) {
            partyIdToGameSession.remove(partyId);
        }
        return true;
    }

    // Parties

    public Optional<PartyComponent> getParty(PlayerRef player) {
        // quick lookup
        var fastParty = playerToPartyId.get(player.getUuid());
        if (fastParty != null) {
            var party = getParty(fastParty).orElse(null);
            // verify the player is still in the party (else stale data may corrupt the
            // quick lookup)
            if (party != null && party.includesPlayer(player)) {
                return Optional.of(party);
            }
        }

        // fallback to a scan across all parties
        for (PartyComponent party : getParties().values()) {
            if (!party.includesPlayer(player))
                continue;

            // rebuild the quick map
            playerToPartyId.put(player.getUuid(), party.getId());
            return Optional.of(party);
        }

        // no party found
        return Optional.empty();
    }

    // simply removes from the party with no side effects
    public void removeParty(UUID playerId) {
        playerToPartyId.remove(playerId);
    }

    public Optional<PartyComponent> getParty(String partyId) {
        return Optional.ofNullable(partyId == null ? null : parties.get(partyId));
    }

    /** Returns the existing party component if present */
    public PartyComponent addParty(PartyComponent party) {
        var existing = parties.put(party.getId(), party);
        // build the player-to-party map - override is fine
        for (var player : party.getAllPlayers()) {
            playerToPartyId.put(player, party.getId());
        }

        return existing;
    }

    public PartyComponent removeParty(String partyId) {
        var party = parties.remove(partyId);
        if (party == null)
            return null;
        // clean the player-to-party map
        for (var player : party.getAllPlayers()) {
            playerToPartyId.remove(player);
        }

        // remove the party from the session
        var sessionId = partyIdToGameSession.remove(partyId);
        if (sessionId == null)
            return party; // not in session before

        var session = sessions.get(sessionId);
        if (session != null) {
            session.getParties().remove(partyId);
        }
        return party;
    }

    /** Gets the session for the party */
    public Optional<GameSession> sessionFor(String partyId) {
        var sessionId = partyIdToGameSession.get(partyId);
        if (sessionId != null) {
            var session = getSession(sessionId).orElse(null);
            // validate that the session does, in fact, contain that party (source of truth
            // accuracy)
            if (session != null && session.getParties().contains(partyId)) {
                return Optional.of(session);
            }
        }

        // fall back to a linear scan of the sessions, repair the lookup map with result
        for (var session : getSessions().values()) {
            if (session.getParties().contains(partyId)) {
                partyIdToGameSession.put(partyId, session.getId());
                return Optional.of(session);
            }
        }

        // remove the incorrect map - the party truly does not have a session they are
        // in. Rip
        partyIdToGameSession.remove(partyId);
        return Optional.empty();
    }

    /**
     * Should only be set via the owner joining/leaving a session, never set
     * manually
     * anywhere or else things WILL become desynced
     * 
     * @return existing session if it was in one
     */
    public String addPartyToSession(String partyId, String sessionId) {
        var party = getParty(partyId).orElse(null);
        if (party == null)
            return null;

        String existing;
        if (sessionId == null) {
            existing = partyIdToGameSession.remove(partyId);
        } else {
            existing = partyIdToGameSession.put(partyId, sessionId);
        }
        removePartyFromSession(partyId, existing);

        var session = sessions.get(sessionId);
        if (session == null)
            return existing;
        // don't use a provided accessor - since we don't want anyone else removing
        // parties
        session.getParties().add(partyId);
        return existing;
    }

    /**
     * Removes a party from the session - should only be called via events
     * 
     * @param partyId
     */
    public void removePartyFromSession(String partyId) {
        var sessionId = partyIdToGameSession.get(partyId);
        if (sessionId == null)
            return;
        removePartyFromSession(partyId, sessionId);
    }

    public void removePartyFromSession(String partyId, String sessionId) {
        var party = getParty(partyId).orElse(null);
        if (party == null)
            return;
        if (sessionId == null)
            return; // session was passed as null

        var session = sessions.get(sessionId);
        if (session == null)
            return;
        session.getParties().remove(partyId);
    }

    @Nullable 
    public Pair<ScheduledFuture<?>, String> getOffline(UUID playerId) {
        if (playerId == null) return null;
        return offlinePlayers.get(playerId);
    }

    public void removeOffline(UUID player) {
        if (player == null) return;
        var pair = offlinePlayers.remove(player);
        if (pair == null) return;
        if (pair.first().isCancelled()) return;
        pair.first().cancel(false); // clean up the future
    }

    public void markOffline(UUID player, String partyId, ScheduledFuture<?> future) {
        // remove any existing
        removeOffline(player);

        // re-mark as offline
        offlinePlayers.put(player, Pair.of(future, partyId));
    }
}
