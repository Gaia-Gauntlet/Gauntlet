package com.gaiagauntlet.gauntlet.plugins.gamestore;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.logger.HytaleLogger;

/**
 * GameStore plugin is a MiniECS system that a Game can opt-into using for state management.
 * 
 * It is not required, technically, but any plugin that requires state may deem it necessary to be implemented
 */
public class GameStorePlugin extends GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String ID = "GameStore";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install() {
        //
    }
}
