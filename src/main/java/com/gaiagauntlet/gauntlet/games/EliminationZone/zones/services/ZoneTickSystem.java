package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.services;

import com.gaiagauntlet.gg.arena.ArenaComponent;
import com.gaiagauntlet.gg.settings.Settings;
import com.gaiagauntlet.gg.store.GlobalStore;
import com.gaiagauntlet.gg.world.WorldController;
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
        var controller = WorldController.of(world);
        if (controller.role() != WorldController.Role.ARENA) {
            return;
        }
        accumulated += dt;
        float interval = Settings.get().get(Settings.ZONE_TICK_SECONDS).floatValue();
        if (accumulated < interval) {
            return;
        }
        float elapsed = accumulated;
        accumulated = 0;
        var globalStore = GlobalStore.find();
        if (globalStore == null) {
            return;
        }
        var game = globalStore.game(controller.gameId()).orElse(null);
        if (game == null || !world.equals(ArenaComponent.TYPE.of(game).world()) || !game.has(ZoneComponent.TYPE)) {
            return;
        }
        var zones = ZoneComponent.TYPE.of(game);
        if (!zones.isActive()) {
            return;
        }
        zones.tick(elapsed);
        zones.paintVoid();
    }
}
