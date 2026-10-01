package com.gaiagauntlet.gauntlet.core.games.interfaces;

import java.util.List;

import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;

/**
 * Standardized game controller asset
 * 
 * Holds all of the game-specific setting configurations
 */
public abstract class GameController {
    

    /**
     * Note: this sets up registries and worlds. This does NOT start the game. The
     * "Start Game" trigger is handled by the game's own implementation
     */
    public abstract void setupGame();

    /**
     * Should be callable at any point to force-end the match that is happening
     */
    public abstract void cleanGame();

    /** Triggered when a player joins back while in this game */
    public abstract void playerJoin();
    
    /** Triggered when a player leaves while in this game */
    public abstract void playerLeave();

    /** Returns the admin tab for configuring / managing this game */
    public abstract AdminTab getAdminTab();

    public abstract List<String> requiredPlugins();
}
