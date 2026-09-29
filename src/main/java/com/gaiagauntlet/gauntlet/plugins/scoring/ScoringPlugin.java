package com.gaiagauntlet.gauntlet.plugins.scoring;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import lombok.Getter;

public class ScoringPlugin extends GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Getter public static final String Id = "ScoringPlugin";

    @Override
    public void install() {
        //
    }

    public void setup(JavaPlugin host) {}
}
