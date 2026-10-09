package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.events;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.utils.LootFountains;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.events.ArenaLoadedEvent;

/** Every fresh arena gets its loot fountains thinned before anyone arrives. */
public final class EZLootHandlers {

    private EZLootHandlers() {
    }

    public static void setup() {
        MatchEventRegistry.register(ArenaLoadedEvent.class, EZController.ID,
                e -> LootFountains.randomize(e.getWorld(), e.getSessionId()));
    }
}
