package com.gaiagauntlet.gauntlet.core.resources;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.server.core.universe.resources.UniverseResourceType;

import lombok.Getter;
import lombok.Setter;

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
            .build();

    @Setter @Getter private static UniverseResourceType<UniverseGauntletResource> resourceType;
    @Getter private Map<String, GameSession> sessions = new ConcurrentHashMap<>();

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
     * This does zero cleanup and may lead to stale/missing/broken data
     */
    public boolean removeSession(GameSession session) {
        if (session == null || session.getId() == null) return false;
        if (!sessions.containsKey(session.getId())) {
            return false; // to remove
        }
        sessions.remove(session.getId());
        return true;
    }
}
