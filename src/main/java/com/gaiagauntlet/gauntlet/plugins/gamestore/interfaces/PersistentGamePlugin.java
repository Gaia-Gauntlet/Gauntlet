package com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Standardized plugin interface
 * 
 * Add params to the methods as-needed. For now, they are empty to prevent param
 * bloat
 */
public interface PersistentGamePlugin extends GamePlugin {
    /** Reads the component data off of the session object and writes into the game store */
    public void read(ComponentAccessor<EntityStore> arenaAccessor, GameSession sessionObject, GameEcs gameStore, String gameId);

    /**
     * Runs on the Arena thread, capture any state from the component here to be applied during the write
     */
    SessionWriter capture(World arenaWorld, GameEcs store, String sessionId);
}
