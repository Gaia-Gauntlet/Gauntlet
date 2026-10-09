package com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces;

import java.util.concurrent.CompletableFuture;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.hypixel.hytale.server.core.universe.world.World;

public interface ArenaManager {
    /** Begins the match officially */
    public CompletableFuture<Void> start(World world, GameEcs game, String sessionID);
}
