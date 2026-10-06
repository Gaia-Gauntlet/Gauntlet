package com.gaiagauntlet.gauntlet.plugins.transfer.components;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import lombok.Getter;
import lombok.Setter;

/** Holds context on the hub world about the current lobby state */
public class TransferComponent implements GameComponent {
    public static final String ID = "TransferComponent";
    @Getter @Setter private static GameComponentType<@NotNull TransferComponent> componentType;
    // the game world1

    private static Deque<PlayerRef> players;
    private static BatchItem progress;

    public World destination;

    public TransferComponent(World world) {
        destination = world;
        players = new ArrayDeque<>();
    }

    public void addPlayers(List<PlayerRef> players) {
        
    }
}
