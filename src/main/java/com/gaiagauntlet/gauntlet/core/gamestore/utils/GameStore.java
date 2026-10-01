package com.gaiagauntlet.gauntlet.core.gamestore.utils;

import com.gaiagauntlet.gauntlet.core.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.core.gamestore.resource.WorldGameStore;
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
    public static GameEcs withStore(ComponentAccessor<EntityStore> accessor, String gameId) {
        return withResource(accessor).getGame(gameId);
    }
    public static GameEcs withStore(World world, String sessionId) {
        return withResource(world).getGame(sessionId);
    }
    
}
