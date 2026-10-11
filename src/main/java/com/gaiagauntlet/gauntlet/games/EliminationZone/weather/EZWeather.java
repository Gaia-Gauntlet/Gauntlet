package com.gaiagauntlet.gauntlet.games.EliminationZone.weather;

import com.gaiagauntlet.gauntlet.games.EliminationZone.weather.events.WeatherEventConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.weather.systems.WeatherTickSystem;
import com.gaiagauntlet.gauntlet.plugins.events.assets.GameEventConfig;
import com.gaiagauntlet.gauntlet.plugins.events.events.SequenceEvent;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class EZWeather {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void setup(JavaPlugin plugin) {
        LOGGER.atInfo().log("Setting up EZGame [Weather]!");
        GameEventConfig.register(WeatherEventConfig.Id, WeatherEventConfig.class, WeatherEventConfig.CODEC);
        plugin.getEventRegistry().register(SequenceEvent.class, WeatherEventConfig.Id, WeatherEventConfig::execute);
        plugin.getEntityStoreRegistry().registerSystem(new WeatherTickSystem());
    }
}
