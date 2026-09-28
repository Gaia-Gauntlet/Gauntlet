package com.gaiagauntlet.gauntlet.plugins.scoring;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.logger.HytaleLogger;

public class ScoringPlugin extends GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "ScoringPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install() {
        //
    }
}
