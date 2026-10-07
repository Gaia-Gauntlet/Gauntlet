package com.gaiagauntlet.gauntlet.core.games.interfaces;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
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
    public abstract CompletableFuture<Void> cleanGame(World hubWorld, GameSession session);

    /** Triggered when a player reconnects in the game */
    public abstract CompletableFuture<Void> playerConnect(World hubWorld, String sessionId, PlayerRef player);

    /** Triggered when a player disconnects in the game - should purely be cleanup logic off the player
     * Runs on whatever thread the player disconnected from. Does NOT exist only on the hub world
    */
    public abstract CompletableFuture<Void> playerDisconnect(World hubWorld, GameSession session, PlayerRef player);
    
    /** Triggered when a player joins the game (either first time or tries to join back) */
    public abstract CompletableFuture<Void> playerJoin(World hubWorld, String sessionId, Collection<PlayerRef> player);

    /** Triggered when a player leaves the game */
    public abstract CompletableFuture<Void> playerLeave(World hubWorld, String sessionId, Collection<PlayerRef> player);

    /** Get the ID of the GameConfigAsset to use by default for this game. */
    public String getDefaultConfigAssetId() {return null;}

    /** Returns the admin tab for configuring / managing this game */
    public abstract AdminTab getAdminTab();

    public abstract List<String> getRequiredPlugins();
}
