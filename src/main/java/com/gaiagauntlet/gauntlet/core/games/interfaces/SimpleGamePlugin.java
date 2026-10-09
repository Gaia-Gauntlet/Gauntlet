package com.gaiagauntlet.gauntlet.core.games.interfaces;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Standardized plugin interface
 * 
 * Add params to the methods as-needed. For now, they are empty to prevent param bloat 
 */
public interface SimpleGamePlugin extends GamePlugin {

    /** Sets up the game after the world is created */
    public void setup(ComponentAccessor<EntityStore> accessor, String sessionId, String gameId);
}
