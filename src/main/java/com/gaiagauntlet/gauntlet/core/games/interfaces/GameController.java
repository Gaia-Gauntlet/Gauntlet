package com.gaiagauntlet.gauntlet.core.games.interfaces;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Standardized game controller asset
 * 
 * Holds all of the game-specific setting configurations
 */
public abstract class GameController {
    public abstract String getId();    

    /**
     * Note: this sets up registries and worlds. This does NOT start the game. The
     * "Start Game" trigger is handled by the game's own implementation
     */
    public abstract CompletableFuture<Void> setupGame(ComponentAccessor<EntityStore> hubAccessor, GameSession session);

    /**
     * Should be callable at any point to force-end the match that is happening
     */
    public abstract CompletableFuture<Void> cleanGame(ComponentAccessor<EntityStore> hubAccessor, GameSession session);

    /** Triggered when a player joins back while in this game */
    public abstract CompletableFuture<Void> playerJoin(ComponentAccessor<EntityStore> hubAccessor, String sessionId, PlayerRef player);
    
    /** Triggered when a player leaves while in this game */
    public abstract CompletableFuture<Void> playerLeave(ComponentAccessor<EntityStore> hubAccessor, String sessionId, PlayerRef player);

    /** Returns the admin tab for configuring / managing this game */
    public abstract AdminTab getAdminTab();

    public abstract List<String> requiredPlugins();
}
