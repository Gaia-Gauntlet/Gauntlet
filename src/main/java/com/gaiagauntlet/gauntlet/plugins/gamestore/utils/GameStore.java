package com.gaiagauntlet.gauntlet.plugins.gamestore.utils;

import java.util.Optional;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.resource.WorldGameStore;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GameStore {
    public static WorldGameStore withResource(ComponentAccessor<EntityStore> accessor) {
        return accessor.getResource(WorldGameStore.getResourceType());
    }
    public static WorldGameStore withResource(World world) {
        return withResource(world.getEntityStore().getStore());   
    }
    public static Optional<GameEcs> withStore(ComponentAccessor<EntityStore> accessor, String sessionId) {
        return withResource(accessor).get(sessionId);
    }
    public static GameEcs ensureStore(ComponentAccessor<EntityStore> accessor, String sessionId) {
        return withResource(accessor).create(sessionId);
    }
    public static GameEcs ensureStore(World accessor, String sessionId) {
        return withResource(accessor).create(sessionId);
    }
    public static Optional<GameEcs> withStore(World world, String sessionId) {
        return withResource(world).get(sessionId);
    }

    /**
     * The session's game store on the hub world, which holds the game state that outlives any one of
     * the game's worlds. Safe to read from any thread.
     */
    public static Optional<GameEcs> withHubStore(String sessionId) {
        return withStore(GauntletUtils.withHubWorld(), sessionId);
    }

    /** Creates the session's game store on the hub world if it has none. Call it on the hub thread. */
    public static GameEcs ensureHubStore(String sessionId) {
        return ensureStore(GauntletUtils.withHubWorld(), sessionId);
    }
    
}
