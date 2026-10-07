package com.gaiagauntlet.gauntlet.games.EliminationZone;

import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.EZBosses;
import com.gaiagauntlet.gauntlet.games.EliminationZone.combat.EZCombat;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spectator.EZSpectator;
import com.gaiagauntlet.gauntlet.games.EliminationZone.weather.EZWeather;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.EZZones;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.GameConfigAsset;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class EZGamePlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public EZGamePlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Starting EZGame!");

        // Start each section - keeps the top-level plugin cleaner this way
        EZSpectator.start(this);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up EZGame!");
        // Register the game
        GameRegistry.registerGame(EZController.ID, EZController::new);

        // Setup each section - keeps the top-level plugin cleaner this way
        EZBosses.setup(this);
        EZCombat.setup(this);
        EZWeather.setup(this);
        EZZones.setup(this);
        EZSpectator.setup(this);

        GameConfigAsset.CODEC.register(EZController.ID, EZGameConfigAsset.class, EZGameConfigAsset.CODEC);
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down EZGame!");
    }
}
