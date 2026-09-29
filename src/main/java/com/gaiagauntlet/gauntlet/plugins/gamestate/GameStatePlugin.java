package com.gaiagauntlet.gauntlet.plugins.gamestate;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import lombok.Getter;

/** Simple state machine handler implementation */
public class GameStatePlugin  extends GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
     @Getter public static final String Id = "GameStatePlugin";

    @Override
    public void install() {
        //
    }

    public void setup(JavaPlugin host) {}
}
