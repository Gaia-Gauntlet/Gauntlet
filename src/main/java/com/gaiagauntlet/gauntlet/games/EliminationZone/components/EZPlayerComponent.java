package com.gaiagauntlet.gauntlet.games.EliminationZone.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import lombok.Getter;
import lombok.Setter;

/** Logic and holds for the current game */
public class EZPlayerComponent implements Component<EntityStore> {
    @Getter @Setter private static ComponentType<EntityStore, EZPlayerComponent> componentType;
    @Getter private String sessionId;
    @Getter private boolean alive = false;

    public EZPlayerComponent() {
    }

    public EZPlayerComponent(String sessionId) {
        this.sessionId = sessionId;
    }

    public EZPlayerComponent(EZPlayerComponent other) {
        this.sessionId = other.sessionId;

    }

    public EZPlayerComponent clone() {
        return new EZPlayerComponent(this);
    }

}