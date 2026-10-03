package com.gaiagauntlet.gauntlet.core;

import com.gaiagauntlet.gauntlet.core.config.GauntletConfig;
import com.gaiagauntlet.gauntlet.core.orchestrator.GauntletOrchestrator;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.riprod.configly.Configly;

public class GauntletCore {
    public static void setup(JavaPlugin plugin) {
        Configly.register(GauntletConfig.TYPE, GauntletConfig.class, GauntletConfig.CODEC);
        GauntletOrchestrator.setupListeners();
    }
}
