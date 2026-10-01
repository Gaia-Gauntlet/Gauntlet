package com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces;

import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

public interface ArenaManager {
    /** Begins the match officially */
    public CompletableFuture<Void> begin(World world);

    public void onPlayerJoin(World world, PlayerRef player);

    public void onPlayerLeave(World world, PlayerRef player);

    /** Tear down the world and any active game immediately */
    public CompletableFuture<Void> terminate(World world);
}
