package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.systems;

import javax.annotation.Nonnull;

import org.joml.Vector3d;

import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components.RisingBlockComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class RisingBlockTickingSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Nonnull
    private final ComponentType<EntityStore, TransformComponent> transformComponentType;

    @Nonnull
    private final ComponentType<EntityStore, RisingBlockComponent> risingComponentType;

    @Nonnull
    private final Query<EntityStore> query;

    public RisingBlockTickingSystem() {
        transformComponentType = TransformComponent.getComponentType();
        risingComponentType = RisingBlockComponent.getComponentType();
        query = Query.and(transformComponentType, risingComponentType);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        final var velocityComponent = archetypeChunk.getComponent(index, risingComponentType);
        final var transformComponent = archetypeChunk.getComponent(index, transformComponentType);

        var distance = new Vector3d(velocityComponent.getVelocity()).mul(dt);
        var position = transformComponent.getPosition();

        transformComponent.setPosition(position.add(distance));
        // technically done?
    }
}
