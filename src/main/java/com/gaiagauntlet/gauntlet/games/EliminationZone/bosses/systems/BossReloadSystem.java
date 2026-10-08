package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.systems;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfigAsset;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossMarkerComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossesComponent;
import com.gaiagauntlet.gauntlet.plugins.config.components.GameConfigComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Collection;

/** Re-links a boss that is out to its entity when the entity loads back in with its chunk. */
public final class BossReloadSystem extends RefSystem<EntityStore> {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return BossMarkerComponent.getComponentType();
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (reason != AddReason.LOAD) {
            return;
        }
        var marker = store.getComponent(ref, BossMarkerComponent.getComponentType());
        if (marker == null) return;

        // TODO: This is unsafe! It currently just finds the first game with EZ config on it.
        //  This should technically work for now but isn't ideal.
        var world = store.getExternalData().getWorld();
        Collection<GameEcs> games = GameStore.withResource(world).getAll();
        GameEcs game = games.stream().filter((g) -> {
            var gameConfigComponent = g.get(GameConfigComponent.getComponentType()).orElse(null);
            return (gameConfigComponent != null) && (gameConfigComponent.getConfig() instanceof EZGameConfigAsset);
        }).findFirst().orElse(null);
        String gameId = GameStore.withResource(world).getId(game);

        var bosses = GameStore.ensureStore(world, gameId).get(BossesComponent.TYPE).orElse(null);
        assert bosses != null;
        bosses.rebind(marker.getBossId(), ref);
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }
}
