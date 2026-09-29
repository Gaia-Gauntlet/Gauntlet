package com.gaiagauntlet.gauntlet.plugins.gamestore;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import lombok.Getter;

/**
 * GameStore plugin is a MiniECS system that a Game can opt-into using for state management.
 * 
 * It is not required, technically, but any plugin that requires state may deem it necessary to be implemented
 */
public class GameStorePlugin extends GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Getter public static final String Id = "GameStore";

    @Override
    public void install() {
        //
    }

    public void setup(JavaPlugin host) {}
}
