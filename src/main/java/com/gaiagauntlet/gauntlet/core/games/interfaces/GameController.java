package com.gaiagauntlet.gauntlet.core.games.interfaces;

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
    abstract void setupGame();

    /**
     * Should be callable at any point to force-end the match that is happening
     */
    abstract void cleanGame();

    /** Triggered when a player joins back while in this game */
    abstract void playerJoin();
    
    /** Triggered when a player leaves while in this game */
    abstract void playerLeave();

}
