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

    @Setter @Getter private static UniverseResourceType<UniverseGauntletResource> resourceType;
    @Nonnull @Getter private Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    @Nonnull @Getter private Map<String, PartyComponent> parties = new ConcurrentHashMap<>();

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
        if (session == null || session.getId() == null) return false;
        if (!sessions.containsKey(session.getId())) {
            return false; // to remove
        }
        sessions.remove(session.getId());
        return true;
    }

    // Parties

    public PartyComponent getParty(String partyId) {
        return parties.get(partyId);
    }

    public PartyComponent createParty(String partyId, UUID owner) {
        var players = new HashSet<UUID>();
        players.add(owner);

        var newParty = new PartyComponent(partyId, players);
        parties.put(partyId, newParty);
        return newParty;
    }

    public PartyComponent removeParty(String partyId) {
        return parties.remove(partyId);
    }

}
