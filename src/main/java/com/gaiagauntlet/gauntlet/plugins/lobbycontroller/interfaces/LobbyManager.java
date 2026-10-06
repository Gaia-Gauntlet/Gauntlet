package com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
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
    /** Cleans up the world - runs on the hub thread*/
    public CompletableFuture<Void> cleanWorld(World arenaWorld);
    /** Adds a player to the lobby world */
    public CompletableFuture<Void> playerTo(World lobbyWorld, PlayerRef player);
    /** Bulk-adds players to the lobby */
    public CompletableFuture<Void> playersTo(World lobbyWorld, Collection<PlayerRef> players);
}
