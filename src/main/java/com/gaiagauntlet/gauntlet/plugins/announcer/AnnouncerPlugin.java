package com.gaiagauntlet.gauntlet.plugins.announcer;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.logger.HytaleLogger;

public class AnnouncerPlugin extends GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "AnnouncerPlugin";

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
