package com.gaiagauntlet.gauntlet.plugins.transfer.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import lombok.Getter;

/** A single batch of players to add */
public class BatchItem {
    
    // where it is going
    @Getter
    World destination;
    Map<UUID, PlayerRef> players;

    public BatchItem(World world) {
        players = new HashMap<>();
    }

    
}
