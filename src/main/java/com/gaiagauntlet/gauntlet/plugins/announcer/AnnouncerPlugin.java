package com.gaiagauntlet.gauntlet.plugins.announcer;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import lombok.Getter;

public class AnnouncerPlugin extends GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Getter public static final String Id = "AnnouncerPlugin";

    public AnnouncerPlugin() {}

    @Override
    public void install() {
        //
    }

    public void setup(JavaPlugin host) {}
}
