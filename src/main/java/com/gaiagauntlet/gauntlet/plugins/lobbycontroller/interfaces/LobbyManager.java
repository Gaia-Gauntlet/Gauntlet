package com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * The Lobby Manager
 * 
 * - Handles the creation of the world
 * - people joining whilst the game is waiting
 * - batching players into the lobby 
 */
public interface LobbyManager {
    public CompletableFuture<World> setupWorld(ComponentAccessor<EntityStore> hubAccessor, String sessionId);
    /** Cleans up the world - runs on the hub thread*/
    public CompletableFuture<Void> cleanWorld(World arenaWorld);
    /** 
     * Triggered when a player connects to the world
     * Must do routing based
     */
    public void onJoin(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor, String sessionId);

    public CompletableFuture<Void> onDisconnect(World arenaWorld, String session, PlayerRef player);
    public CompletableFuture<Void> onLeave(World arenaWorld, String session, Collection<PlayerRef> player);
    public CompletableFuture<Transform> locationFor(PlayerRef player, World world, String sessionId);
}
