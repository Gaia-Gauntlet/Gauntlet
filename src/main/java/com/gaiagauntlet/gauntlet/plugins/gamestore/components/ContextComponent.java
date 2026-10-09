package com.gaiagauntlet.gauntlet.plugins.gamestore.components;

import org.jetbrains.annotations.NotNull;

import com.hypixel.hytale.server.core.universe.world.World;

import lombok.Getter;
import lombok.Setter;

/**
 * simple MTO for the session. Not as much churn as an ECS since this is just a
 * mini-ecs without any form of ref system
 */
public class ContextComponent implements GameComponent {
    public static final String ID = "ContextComponent";
    @Getter @Setter private static GameComponentType<@NotNull ContextComponent> componentType;
    @Getter
    private World world;
    @Getter
    private String gameId;
    @Getter
    private String sessionId;

    public ContextComponent(World world, String gameId, String sessionId) {
        this.world = world;
        this.gameId = gameId;
        this.sessionId = sessionId;
    }
}
