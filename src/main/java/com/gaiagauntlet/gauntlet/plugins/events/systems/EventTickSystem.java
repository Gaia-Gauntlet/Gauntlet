package com.gaiagauntlet.gauntlet.plugins.events.systems;

import com.gaiagauntlet.gauntlet.plugins.events.components.GameEventComponent;
import com.gaiagauntlet.gauntlet.plugins.events.utils.EventUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.GameTickingSystem;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameQuery;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EventTickSystem extends GameTickingSystem {

    @Override
    public GameQuery getGameQuery() {
        return GameQuery.of(GameEventComponent.getComponentType());
    }

    @Override
    public void tick(float dt, String sessionId, GameEcs gameStore, Store<EntityStore> store) {
        var events = EventUtils.get(gameStore).orElse(null);
        if (events == null || events.isPaused()) {
            return;
        }
        events.incrementTimer(dt);

        var scheduled = events.popDue();
        if (scheduled != null) {
            EventUtils.fire(gameStore, scheduled.getGameConfig());
        }

        var random = events.drawRandomIfDue();
        if (random != null) {
            EventUtils.fire(gameStore, random.getGameConfig());
        }
    }
}
