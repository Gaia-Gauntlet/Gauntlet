package com.gaiagauntlet.gauntlet.plugins.lobbycontroller.components;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.core.session.components.SessionComponentType;
import com.gaiagauntlet.gauntlet.core.session.registry.SessionRegistry;
import com.hypixel.hytale.server.core.universe.world.World;

import lombok.Getter;

/** Holds context on the hub world about the current lobby state */
public class LobbyComponent implements SessionComponent {
    public static final String ID = "LobbyComponent";
    @Getter
    private static SessionComponentType<@NotNull LobbyComponent> componentType = SessionRegistry.register(ID,
            LobbyComponent.class);
    
    /** The game world */
    @Getter
    private final World world;

    public LobbyComponent(World world) {
        this.world = world;
    }
}
