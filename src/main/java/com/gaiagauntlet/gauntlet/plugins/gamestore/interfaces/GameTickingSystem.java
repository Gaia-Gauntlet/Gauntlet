package com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameQuery;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Runs on the Hub thread, applies the changes to the GameSession
 */
public abstract class GameTickingSystem extends TickingSystem<EntityStore> {
    public abstract GameQuery getGameQuery();

    public abstract void tick(float dt, String sessionId, GameEcs game, Store<EntityStore> store);

    @Override
    public final void tick(float dt, int systemIndex, @Nonnull Store<EntityStore> store) {
        GameStore.withResource(store).forEachMatch(getGameQuery(), (sessionId, game) -> {
            try {
                tick(dt, sessionId, game, store);
            } catch (RuntimeException e) {
                GaiaLog.atWarning().withCause(e).log(getClass().getSimpleName() + " failed for session " + sessionId);
            }
        });
    }

}
