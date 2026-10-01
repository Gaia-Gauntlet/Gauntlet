package com.gaiagauntlet.gauntlet.core;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.core.admin.AdminLog;
import com.gaiagauntlet.gauntlet.core.components.PlayerComponent;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.resources.UniverseGameResource;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

/**
 *  Stateless getters or setters
 */
public class GauntletUtils {
    /** Shortcut for getting the universe resource with all the sessions */
    public static UniverseGameResource withResource() {
        return Universe.get().getResource(UniverseGameResource.getResourceType());
    }

    @Nonnull
    public static Optional<GameSession> sessionFor(@Nonnull String id) {
        return withResource().getSession(id);
    }

    @Nonnull
    public static Optional<GameSession> sessionFor(@Nonnull PlayerRef player) {
        var comp = playerFor(player);
        if (comp.isPresent() && comp.get().getActiveSession() != null)
            return sessionFor(comp.get().getActiveSession());
        return Optional.empty();
    }

    @Nonnull
    public static Optional<PlayerComponent> playerFor(@Nonnull PlayerRef player) {
        return Optional.ofNullable(player.getComponentConcurrent(PlayerComponent.getComponentType()));
    }
}
