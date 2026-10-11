package com.gaiagauntlet.gauntlet.games.EliminationZone.weather.systems;

import com.gaiagauntlet.gauntlet.games.EliminationZone.weather.components.CurrentWeatherComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.weather.utils.WeatherUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.GameTickingSystem;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameQuery;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class WeatherTickSystem extends GameTickingSystem {

    @Override
    public GameQuery getGameQuery() {
        return GameQuery.of(CurrentWeatherComponent.getComponentType());
    }

    @Override
    public void tick(float dt, String sessionId, GameEcs game, Store<EntityStore> store) {
        var weather = WeatherUtils.current(game).orElse(null);
        if (weather != null && weather.countDown(dt)) {
            WeatherUtils.doStop(game);
        }
    }
}
