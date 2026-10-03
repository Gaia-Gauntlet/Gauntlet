package com.gaiagauntlet.gauntlet.plugins.gamestore;

import java.util.List;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.core.events.GauntletEvents;
import com.gaiagauntlet.gauntlet.core.games.interfaces.SimpleGamePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.events.GameEventHandler;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * GameStore plugin is a MiniECS system that a Game can opt-into using for state
 * management.
 * 
 * It is not required, technically, but any plugin that requires state may deem
 * it necessary to be implemented
 */
public class GameStorePlugin implements SimpleGamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "GameStore";

    @Override
    public void init(JavaPlugin host) {
        // intercepts any session plugins and persists their state
        GauntletEvents.on(GauntletEvent.GameEnd.class, GameEventHandler::GameEndHandler);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<String> getDependencies() {
        return List.of();
    }

    @Override
    public void setup(ComponentAccessor<EntityStore> accessor, String gameId) {
        // setup the gameStore
    }
}
