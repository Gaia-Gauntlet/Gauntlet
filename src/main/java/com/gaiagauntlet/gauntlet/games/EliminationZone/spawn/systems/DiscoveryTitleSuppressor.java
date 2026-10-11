package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.systems;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.games.EliminationZone.components.EZPlayerComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.DiscoverZoneEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class DiscoveryTitleSuppressor extends EntityEventSystem<EntityStore, DiscoverZoneEvent.Display> {

    public DiscoveryTitleSuppressor() {
        super(DiscoverZoneEvent.Display.class);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), EZPlayerComponent.getComponentType());
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull DiscoverZoneEvent.Display event) {
        event.setCancelled(true);
    }
}
