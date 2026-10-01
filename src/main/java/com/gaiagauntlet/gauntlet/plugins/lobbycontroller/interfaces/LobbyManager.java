package com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces;

import java.util.Collection;
import java.util.concurrent.CompletableFuture.AsynchronousCompletionTask;
import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

/**
 * The Lobby Manager
 * 
 * - Handles the creation of the world
 * - people joining whilst the game is waiting
 * - batching players into the lobby 
 */
public interface LobbyManager {
    public CompletableFuture<World> setupWorld();
    /** Adds a player to the lobby world */
    public AsynchronousCompletionTask playerTo(World lobbyWorld, PlayerRef player);
    /** Bulk-adds players to the lobby */
    public AsynchronousCompletionTask playersTo(World lobbyWorld, Collection<PlayerRef> players);
}
