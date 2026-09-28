package com.gaiagauntlet.gauntlet.plugins.teams;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.logger.HytaleLogger;

public class TeamsPlugin  extends GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

     public static final String ID = "TeamsPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install() {
        //
    }
}
