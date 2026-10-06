package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.services;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneComponent;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.GameConfig;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Collection;

/** Advances the zone sequence of the game whose arena this world is, a few times a second. */
public final class ZoneTickSystem extends TickingSystem<EntityStore> {

    private float accumulated;

    @Override
    public void tick(float dt, int systemIndex, @Nonnull Store<EntityStore> store) {
        var world = store.getExternalData().getWorld();

        accumulated += dt;

        // TODO: This is unsafe! It currently just finds the first game with EZ config on it.
        //  This should technically work for now but isn't ideal.
        Collection<GameEcs> games = GameStore.withResource(world).getAll();
        GameEcs game = games.stream().filter((g) -> {
            GameConfig config = g.get(GameConfig.TYPE).orElse(null);
            if (config == null) return false;
            return config instanceof EZGameConfig;
        }).findFirst().orElse(null);

        if (game == null) return;

        var config = game.get(GameConfig.TYPE).orElse(null);
        if (!(config instanceof EZGameConfig gameConfig)) return;

        float interval = gameConfig.getZoneTickSeconds();
        if (accumulated < interval) return;

        float elapsed = accumulated;
        accumulated = 0;

        String gameId = GameStore.withResource(world).getId(game);

        var zones = GameStore.ensureStore(world, gameId).ensure(ZoneComponent.TYPE, ZoneComponent::new);
        if (!zones.isActive()) return;

        zones.tick(elapsed);
        zones.paintVoid(gameId);
    }
}
