package com.gaiagauntlet.gauntlet.plugins.announcer;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.util.logging.Level;

public class AnnouncerPlugin extends GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String ID = "AnnouncerPlugin";

    public AnnouncerPlugin() {
        
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install() {
        //
    }
}
