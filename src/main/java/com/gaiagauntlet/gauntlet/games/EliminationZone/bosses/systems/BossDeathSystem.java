package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.systems;

import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossMarkerComponent;
import com.gaiagauntlet.gg.store.GlobalStore;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

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
        var globalStore = GlobalStore.find();
        if (marker == null || globalStore == null) {
            return;
        }
        globalStore.game(marker.gameId()).ifPresent(game ->
                BossSpawner.onDefeated(game, store.getExternalData().getWorld(), marker.bossId()));
    }
}
