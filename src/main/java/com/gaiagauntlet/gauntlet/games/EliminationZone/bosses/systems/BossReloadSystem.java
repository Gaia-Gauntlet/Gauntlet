package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.systems;

import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossMarkerComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossesComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

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

        GameStore.withStore(store.getExternalData().getWorld(), marker.getSessionId())
                .flatMap(game -> game.get(BossesComponent.TYPE))
                .ifPresent(bosses -> bosses.rebind(marker.getBossId(), ref));
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }
}
