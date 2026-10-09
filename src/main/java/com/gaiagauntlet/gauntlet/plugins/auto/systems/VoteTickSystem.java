package com.gaiagauntlet.gauntlet.plugins.auto.systems;

import com.gaiagauntlet.gauntlet.plugins.auto.components.VoteComponent;
import com.gaiagauntlet.gauntlet.plugins.auto.utils.VoteUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.GameTickingSystem;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameQuery;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class VoteTickSystem extends GameTickingSystem {
    // only tick when there is an active vote component
    private static final GameQuery query = GameQuery.of(VoteComponent.getComponentType());

    @Override
    public GameQuery getGameQuery() {
        return query;
    }

    @Override
    public void tick(float dt, String sessionId, GameEcs game, Store<EntityStore> store) {
        var vote = game.get(VoteComponent.getComponentType()).orElse(null);
        var remaining = vote.decrementTimer(dt);
        if (remaining <= 0) {
            VoteUtils.close(vote, sessionId);
        }
    }
    
}
