package com.gaiagauntlet.gauntlet.plugins.lobbycontroller;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.util.logging.Level;

/**
 * A lobby controller plugin that lets you opt-into lobby logic.
 */
public class LobbyControllerPlugin extends GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "LobbyControllerPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install() {
        //
    }
}
