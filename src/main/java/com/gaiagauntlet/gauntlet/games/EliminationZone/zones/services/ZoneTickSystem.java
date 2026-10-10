package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.services;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.GameTickingSystem;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameQuery;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;


/** Advances the zone sequence of the game whose arena this world is, a few times a second. */
public final class ZoneTickSystem extends GameTickingSystem {

    public ZoneTickSystem() {

    }

    @Override
    public void tick(float dt, String sessionId, GameEcs game, Store<EntityStore> store) {
        var zoneComponent = game.ensure(ZoneComponent.TYPE, () -> new ZoneComponent(sessionId));
        if (!zoneComponent.isActive()) return;
        
        var conf = EZGameConfig.get(game);
        if (!zoneComponent.increment(dt, conf.getZoneTickSeconds())) {
            return;
        }

        zoneComponent.paintVoid(sessionId);
    }

    @Override
    public GameQuery getGameQuery() {
        return GameQuery.of(ZoneComponent.TYPE);
    }
}
