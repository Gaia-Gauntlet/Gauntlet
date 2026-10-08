package com.gaiagauntlet.gauntlet.games.EliminationZone.combat;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class EZCombat {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void setup(JavaPlugin plugin) {
        LOGGER.atInfo().log("Setting up EZGame [Combat]!");
        // register any and all combat stuff here
    }
}
