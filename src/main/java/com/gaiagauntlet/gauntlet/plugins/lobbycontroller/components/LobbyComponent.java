package com.gaiagauntlet.gauntlet.plugins.lobbycontroller.components;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.hypixel.hytale.server.core.universe.world.World;

import lombok.Getter;
import lombok.Setter;

/** Holds context on the hub world about the current lobby state */
public class LobbyComponent implements GameComponent {
    public static final String ID = "LobbyComponent";
    @Getter @Setter private static GameComponentType<@NotNull LobbyComponent> componentType;
    /** The game world */
    @Getter private final World world;

    public LobbyComponent(World world) {
        this.world = world;
    }
}
