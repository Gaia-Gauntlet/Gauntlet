package com.gaiagauntlet.gauntlet.core;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.components.PlayerComponent;
import com.gaiagauntlet.gauntlet.core.resources.UniverseGameResource;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

/**
 * The very thin big boi router
 * 90% of the business logic for this should exist within the GameController
 * 
 * The Orchestrator is simply there to route and standardize implementations
 */
public class GauntletOrchestrator {











    public UniverseGameResource withResource() {
        return Universe.get().getResource(UniverseGameResource.getResourceType());
    }

    @Nonnull
    public Optional<GameSession> sessionFor(@Nonnull String id) {
        return withResource().getSession(id);
    }

    @Nonnull
    public Optional<GameSession> sessionFor(@Nullable PlayerRef player) {
        var comp = playerFor(player);
        if (comp.isPresent() && comp.get().getActiveSession() != null)
            return sessionFor(comp.get().getActiveSession());
        return Optional.empty();
    }

    @Nonnull
    public Optional<PlayerComponent> playerFor(@Nullable PlayerRef player) {
        return Optional.ofNullable(player.getComponentConcurrent(PlayerComponent.getComponentType()));
    }
}
