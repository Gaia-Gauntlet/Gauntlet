package com.gaiagauntlet.gauntlet.games.EliminationZone;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class EZGamePlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public EZGamePlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Starting Gauntlet [SCORING]!");
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up Gauntlet [SCORING]!");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down Gauntlet [SCORING]!");
    }
}
