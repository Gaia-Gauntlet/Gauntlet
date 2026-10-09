package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.systems;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components.RisingBlockComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class RisingBlockSystem extends EntityTickingSystem<EntityStore> {

    private final Query<EntityStore> query = Query.and(TransformComponent.getComponentType(),
            RisingBlockComponent.getComponentType());

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var rising = archetypeChunk.getComponent(index, RisingBlockComponent.getComponentType());
        var transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        if (rising == null || transform == null) return;

        var position = transform.getPosition();
        position.y = Math.min(rising.getTargetY(), position.y + rising.getSpeed() * dt);
    }
}
