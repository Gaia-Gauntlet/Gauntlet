package com.gaiagauntlet.gauntlet.plugins.transfer.components;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.hypixel.hytale.server.core.universe.world.World;

import lombok.Getter;
import lombok.Setter;

/**
 * Holds context on the hub world about the current lobby state
 * 
 * Keeps track of in-progress transfers and allows for recovery on failure
 */
public class TransferComponent implements GameComponent {
    public static final String ID = "TransferComponent";
    @Getter
    @Setter
    private static GameComponentType<@NotNull TransferComponent> componentType;
    // the game world1

    private static Set<BatchItem> batches;

    public World destination;

    public TransferComponent(World world) {
        destination = world;
        batches = ConcurrentHashMap.newKeySet();
    }

    public World getDestination() {
        return destination;
    };

    public void add(BatchItem batch) {
        batches.add(batch);
    }
}
