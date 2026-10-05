package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.events;

/** Every fresh arena gets its loot fountains thinned before anyone arrives. */
public final class LootHandlers {

    private LootHandlers() {
    }

    public static void register() {
        // TODO: Trigger loot fountain randomisation on arena loaded.
//        GauntletEventRegistry.on(ArenaLoadedEvent.class, e -> LootFountains.randomize(e.world()));
    }
}
