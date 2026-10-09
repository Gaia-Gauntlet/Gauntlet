package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.services;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneComponent;
import com.gaiagauntlet.gauntlet.plugins.config.components.GameConfigComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
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
            var gameConfigComponent = g.get(GameConfigComponent.getComponentType()).orElse(null);
            return (gameConfigComponent != null) && (gameConfigComponent.getConfig() instanceof EZGameConfig);
        }).findFirst().orElse(null);
        String gameId = GameStore.withResource(world).getId(game);

        if (game == null) return;

        var configComp = game.get(GameConfigComponent.getComponentType()).orElse(null);
        if (configComp == null) return;
        var config = configComp.getConfig();
        if (!(config instanceof EZGameConfig gameConfig)) return;

        float interval = gameConfig.getZoneTickSeconds();
        if (accumulated < interval) return;

        float elapsed = accumulated;
        accumulated = 0;

        var zones = GameStore.ensureStore(world, gameId).ensure(ZoneComponent.TYPE, ZoneComponent::new);
        if (!zones.isActive()) return;

        zones.tick(elapsed);
        zones.paintVoid(gameId);
    }
}
