package com.gaiagauntlet.gauntlet.plugins.lobbycontroller.components;

import java.util.UUID;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import lombok.Getter;
import lombok.Setter;

public class PlayerMarker implements Component<EntityStore> {
    @Getter  @Setter public static ComponentType<EntityStore, PlayerMarker> componentType;

    /** Desired world check just to be sure the transfer was done correctly */
    @Getter
    private UUID desiredWorld;
    /** Validated gameId to run the controller for */
    @Getter 
    private String sessionId;

    public PlayerMarker(String sessionId, UUID desiredWorld) {
        this.desiredWorld = desiredWorld;
        this.sessionId = sessionId;
    }

    public PlayerMarker() {
        this(null, null);
    }

    public PlayerMarker(PlayerMarker other) {
        desiredWorld = other.desiredWorld;
        sessionId = other.sessionId;
    }
    
    public PlayerMarker clone() {
        return new PlayerMarker(this);
    }
}
