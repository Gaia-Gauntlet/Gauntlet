package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.systems;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.BossSpawner;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossMarkerComponent;
import com.gaiagauntlet.gauntlet.plugins.config.components.GameConfigComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Collection;

/** Reports a marked boss's death to its game. */
public final class BossDeathSystem extends DeathSystems.OnDeathSystem {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return BossMarkerComponent.getComponentType();
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent death, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var marker = store.getComponent(ref, BossMarkerComponent.getComponentType());
        if (marker == null) return;

        // TODO: This is unsafe! It currently just finds the first game with EZ config on it.
        //  This should technically work for now but isn't ideal.
        var world = store.getExternalData().getWorld();
        Collection<GameEcs> games = GameStore.withResource(world).getAll();
        GameEcs game = games.stream().filter((g) -> {
            var gameConfigComponent = g.get(GameConfigComponent.getComponentType()).orElse(null);
            return (gameConfigComponent != null) && (gameConfigComponent.getConfig() instanceof EZGameConfig);
        }).findFirst().orElse(null);
        String gameId = GameStore.withResource(world).getId(game);

        BossSpawner.onDefeated(gameId, world, marker.getBossId());
    }
}
