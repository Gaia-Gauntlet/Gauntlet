package com.gaiagauntlet.gauntlet.core.games.interfaces;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Standardized plugin interface
 * 
 * Add params to the methods as-needed. For now, they are empty to prevent param bloat 
 */
public interface PersistentGamePlugin extends GamePlugin {
    /** Installs the plugin into a game */
    public void setup(ComponentAccessor<EntityStore> accessor, GameSession sessionObject, String gameId);

    /** Writes any state onto the session during the transition out of the game */
    public void writeSession(ComponentAccessor<EntityStore> accessor, GameSession sessionObject, String gameId);
}
