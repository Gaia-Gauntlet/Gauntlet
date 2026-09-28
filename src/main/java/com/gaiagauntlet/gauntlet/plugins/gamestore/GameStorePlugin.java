package com.gaiagauntlet.gauntlet.plugins.gamestore;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import java.util.logging.Level;

/**
 * GameStore plugin is a MiniECS system that a Game can opt-into using for state management.
 * 
 * It is not required, technically, but any plugin that requires state may deem it necessary to be implemented
 */
public class GameStorePlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public GameStorePlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Starting Gauntlet [GAME STORE]!");
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up Gauntlet [GAME STORE]!");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down Gauntlet [GAME STORE]!");
    }
}
