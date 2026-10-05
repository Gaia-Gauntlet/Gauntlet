package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.services;

import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/** Advances the zone sequence of the game whose arena this world is, a few times a second. */
public final class ZoneTickSystem extends TickingSystem<EntityStore> {

    private float accumulated;

    @Override
    public void tick(float dt, int systemIndex, @Nonnull Store<EntityStore> store) {
        var world = store.getExternalData().getWorld();

        accumulated += dt;
        // TODO: Game settings
//        float interval = Settings.get().get(Settings.ZONE_TICK_SECONDS).floatValue();
        float interval = 2;
        if (accumulated < interval) return;

        float elapsed = accumulated;
        accumulated = 0;

        // TODO: Get game for store/world
        String game = "";

        var zones = GameStore.ensureStore(world, game).ensure(ZoneComponent.TYPE, ZoneComponent::new);
        if (!zones.isActive()) return;

        zones.tick(elapsed);
        zones.paintVoid();
    }
}
