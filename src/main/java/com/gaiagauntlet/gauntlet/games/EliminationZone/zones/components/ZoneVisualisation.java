package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/** Marks a player who asked to see zone outlines. */
public final class ZoneVisualisation implements Component<EntityStore> {

    private static ComponentType<EntityStore, ZoneVisualisation> type;

    public ZoneVisualisation() {
    }

    public static void setType(@Nonnull ComponentType<EntityStore, ZoneVisualisation> componentType) {
        type = componentType;
    }

    @Nonnull
    public static ComponentType<EntityStore, ZoneVisualisation> getComponentType() {
        return type;
    }

    @Override
    public ZoneVisualisation clone() {
        return new ZoneVisualisation();
    }
}
