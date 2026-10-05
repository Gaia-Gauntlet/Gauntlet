package com.gaiagauntlet.gauntlet.core.resources;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.gaiagauntlet.gauntlet.core.party.components.PartyComponent;
import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.server.core.universe.resources.UniverseResourceType;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;

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

    @Setter
    @Getter
    private static UniverseResourceType<UniverseGauntletResource> resourceType;
    @Nonnull
    @Getter
    private Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    @Nonnull
    @Getter
    private Map<String, PartyComponent> parties = new ConcurrentHashMap<>();

    private static Map<String, String> partyIdToGameSession = new ConcurrentHashMap<>();

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
        return true;
    }

    // Parties

    public Optional<PartyComponent> getParty(String partyId) {
        return Optional.ofNullable(parties.get(partyId));
    }

    /** Returns the existing party component if present */
    public PartyComponent addParty(PartyComponent party) {
        var existing = parties.put(party.getId(), party);
        return existing;
    }

    public PartyComponent removeParty(String partyId) {
        return parties.remove(partyId);
    }

    public Optional<GameSession> sessionFor(String partyId) {
        var sessionId = partyIdToGameSession.get(partyId);
        return getSession(sessionId);
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
            existing = partyIdToGameSession.put(partyId, sessionId);
        } else {
            existing = partyIdToGameSession.remove(partyId);
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
        var session = sessions.get(sessionId);
        if (session == null)
            return;
        session.getParties().remove(partyId);

    }

}
