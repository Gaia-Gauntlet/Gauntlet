package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.systems;

import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.BossSpawner;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossMarkerComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
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
        if (marker == null) return;

        var sessionId = marker.getSessionId();
        var world = store.getExternalData().getWorld();
        if (GameStore.withStore(world, sessionId).isEmpty()) return;

        BossSpawner.onDefeated(sessionId, world, marker.getBossId());
    }
}
