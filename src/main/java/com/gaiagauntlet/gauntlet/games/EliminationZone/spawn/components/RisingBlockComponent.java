package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components;

import javax.annotation.Nonnull;

import org.joml.Vector3d;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import lombok.Setter;

public class RisingBlockComponent implements Component<EntityStore> {
    @Getter
    @Setter
    public static ComponentType<EntityStore, RisingBlockComponent> componentType;

        @Getter
    private final Vector3d velocity;

    public RisingBlockComponent() {
        this(new Vector3d());
    }

    public RisingBlockComponent(Vector3d velocity) {
        this.velocity = velocity;
    }

    @Nonnull
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public RisingBlockComponent clone() {
        final var risingBlockComponent = new RisingBlockComponent(velocity);
        return risingBlockComponent;
    }
}
