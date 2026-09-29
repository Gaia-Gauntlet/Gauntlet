package com.gaiagauntlet.gauntlet.plugins.lobbycontroller;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import lombok.Getter;

/**
 * A lobby controller plugin that lets you opt-into lobby logic.
 */
public class LobbyControllerPlugin extends GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Getter public static final String Id = "LobbyControllerPlugin";

    @Override
    public void install() {
        //
    }

    public void setup(JavaPlugin host) {}
}
