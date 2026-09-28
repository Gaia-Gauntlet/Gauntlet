package com.gaiagauntlet.gauntlet.plugins.announcer;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.util.logging.Level;

public class AnnouncerPlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static AnnouncerPlugin instance;

    public AnnouncerPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Starting Gauntlet [ANNOUNCER]!");
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up Gauntlet [ANNOUNCER]!");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down Gauntlet [ANNOUNCER]!");
    }
}
