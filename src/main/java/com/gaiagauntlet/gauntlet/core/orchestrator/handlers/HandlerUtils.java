package com.gaiagauntlet.gauntlet.core.orchestrator.handlers;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.components.PlayerComponent;
import com.gaiagauntlet.gauntlet.core.resources.UniverseGauntletResource;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Bunch of utilities for the handlers so I don't have to repeat myself a
 * thousand times
 */
public class HandlerUtils {
    public static UniverseGauntletResource withResource() {
        return GauntletUtils.withResource();
    }

    @Nonnull
    public static Optional<GameSession> sessionFor(@Nonnull String id) {
        return GauntletUtils.sessionFor(id);
    }

    @Nonnull
    public static Optional<GameSession> sessionFor(@Nonnull PlayerRef player) {
        return GauntletUtils.sessionFor(player);
    }

    @Nonnull
    public static Optional<PlayerComponent> playerFor(@Nonnull PlayerRef player) {
        return GauntletUtils.playerFor(player);
    }
}
