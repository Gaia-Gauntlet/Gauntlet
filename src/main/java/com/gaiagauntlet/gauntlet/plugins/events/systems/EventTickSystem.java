package com.gaiagauntlet.gauntlet.plugins.events.systems;

import java.util.concurrent.TimeUnit;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.plugins.events.components.GameEventComponent;
import com.gaiagauntlet.gauntlet.plugins.events.components.SequenceHolder;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.events.events.SequenceEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.GameTickingSystem;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameQuery;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EventTickSystem extends GameTickingSystem {

    @Override
    public GameQuery getGameQuery() {
        return GameQuery.of(GameEventComponent.getComponentType());
    }

    @Override
    public void tick(float dt, String sessionId, GameEcs gameStore, Store<EntityStore> store) {
        var randomEventAccumulator = gameStore.get(GameEventComponent.getComponentType()).orElse(null);
        if (randomEventAccumulator == null) {
            HytaleLogger.getLogger().atWarning().atMostEvery(30, TimeUnit.SECONDS)
                    .log("Query failed to filter to only RandomGameEvent Worlds");
            return;
        }

        var accumulated = randomEventAccumulator.incrementTimer(dt);
        // check random event
        var nextEventTime = randomEventAccumulator.getNextEventTime();
        if (accumulated >= nextEventTime) {
            var evt = randomEventAccumulator.popEvent();
            if (evt != null) {
                processEvent(sessionId, gameStore, evt);
            }
        }

        // check random events
        var nextRandomEvent = randomEventAccumulator.getNextRandomEventTime();
        if (accumulated > nextRandomEvent) {
            var randomEvt = randomEventAccumulator.nextRandomEvent();
            if (randomEvt != null) {
                processEvent(sessionId, gameStore, randomEvt);
            }
        }
    }

    /** Processes and emits the event */
    private void processEvent(String sessionId, GameEcs store, SequenceHolder evtHolder) {
        var event = new SequenceEvent(sessionId, evtHolder.getGameConfig());

        final var dispatcher = HytaleServer.get().getEventBus().dispatchFor(SequenceEvent.class,
                evtHolder.getID());

        if (dispatcher.hasListener()) {
            GaiaLog.atInfo().withSession(sessionId).log("Dispatching game event: " + evtHolder.getID());
            dispatcher.dispatch(event);
        }

    }

}
