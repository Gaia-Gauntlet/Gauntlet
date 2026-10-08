package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;

/** Marks a player who asked to see zone outlines. */
public final class ZoneVisualisationComponent implements Component<EntityStore> {

    @Getter @Setter private static ComponentType<EntityStore, ZoneVisualisationComponent> componentType;

    public ZoneVisualisationComponent() {}

    @Override
    public ZoneVisualisationComponent clone() {
        return new ZoneVisualisationComponent();
    }
}
