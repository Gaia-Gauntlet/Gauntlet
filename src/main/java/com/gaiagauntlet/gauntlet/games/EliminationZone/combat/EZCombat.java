package com.gaiagauntlet.gauntlet.games.EliminationZone.combat;

import com.gaiagauntlet.gauntlet.games.EliminationZone.combat.systems.SpawnDamagePreventionSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class EZCombat {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void setup(JavaPlugin plugin) {
        LOGGER.atInfo().log("Setting up EZGame [Combat]!");

        plugin.getEntityStoreRegistry().registerSystem(new SpawnDamagePreventionSystem());
    }
}
