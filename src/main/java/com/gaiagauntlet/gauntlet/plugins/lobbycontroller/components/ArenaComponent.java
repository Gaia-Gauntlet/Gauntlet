package com.gaiagauntlet.gauntlet.plugins.lobbycontroller.components;

import com.gaiagauntlet.gauntlet.core.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.core.gamestore.components.GameComponentType;
import com.hypixel.hytale.server.core.universe.world.World;

import lombok.Getter;
import lombok.Setter;

public class ArenaComponent implements GameComponent {
    public static final String ID = "ArenaComponent";
    @Getter @Setter private static GameComponentType<ArenaComponent> componentType;
    // the game world
    @Getter
    private World world;

    ArenaComponent(World world) {
        this.world = world;
    }
}
