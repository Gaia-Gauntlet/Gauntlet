package com.gaiagauntlet.gauntlet.plugins.teams;

import com.gaiagauntlet.gauntlet.GauntletPlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import lombok.Getter;

import java.util.logging.Level;

public class TeamsPlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static TeamsPlugin instance;

    public TeamsPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static TeamsPlugin get() {
        return instance;
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Starting Gauntlet [TEAMS]!");
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up Gauntlet [TEAMS]!");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down Gauntlet [TEAMS]!");
    }
}
