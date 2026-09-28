package com.gaiagauntlet.gauntlet.plugins.gamestate;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.util.logging.Level;

/** Simple state machine handler implementation */
public class GameStatePlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static GameStatePlugin instance;

    public GameStatePlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Starting Gauntlet [GAME STATE]!");
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up Gauntlet [GAME STATE]!");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down Gauntlet [GAME STATE]!");
    }
}
