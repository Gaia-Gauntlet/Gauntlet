package com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces;

import java.util.concurrent.CompletableFuture.AsynchronousCompletionTask;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

public interface ArenaManager {
    /** Begins the match officially */
    public AsynchronousCompletionTask begin(World world);

    public void onPlayerJoin(World world, PlayerRef player);

    public void onPlayerLeave(World world, PlayerRef player);

    /** Tear down the world and any active game immediately */
    public AsynchronousCompletionTask terminate(World world);
}
