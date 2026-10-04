package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.events;

import com.gaiagauntlet.gg.events.ArenaLoadedEvent;
import com.gaiagauntlet.gg.events.Events;

/** Every fresh arena gets its loot fountains thinned before anyone arrives. */
public final class LootHandlers {

    private LootHandlers() {
    }

    public static void register() {
        Events.on(ArenaLoadedEvent.class, e -> LootFountains.randomize(e.world()));
    }
}
