package com.gaiagauntlet.gauntlet.plugins.teams;

import com.gaiagauntlet.gauntlet.GauntletPlugin;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import lombok.Getter;

import java.util.logging.Level;

public class TeamsPlugin  extends GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

     private static final String ID = "TeamsPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install() {
        //
    }
}
