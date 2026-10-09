package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components;

import javax.annotation.Nonnull;

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
    private final double speed;
    @Getter
    private final double targetY;

    public RisingBlockComponent() {
        this(0, 0);
    }

    public RisingBlockComponent(double speed, double targetY) {
        this.speed = speed;
        this.targetY = targetY;
    }

    @Nonnull
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public RisingBlockComponent clone() {
        return new RisingBlockComponent(speed, targetY);
    }
}
