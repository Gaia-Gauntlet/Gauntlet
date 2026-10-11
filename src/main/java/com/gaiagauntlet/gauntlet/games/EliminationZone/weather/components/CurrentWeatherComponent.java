package com.gaiagauntlet.gauntlet.games.EliminationZone.weather.components;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

public final class CurrentWeatherComponent implements GameComponent {
    public final static String ID = "CurrentWeatherComponent";
    @Getter
    private static GameComponentType<CurrentWeatherComponent> componentType = GameComponentRegistry.register(ID,
            CurrentWeatherComponent.class);
    @Getter
    @Setter
    private String weatherId;
    @Getter
    @Setter
    private Double remainingSeconds;

    public CurrentWeatherComponent() {
    }

    public CurrentWeatherComponent(String weatherId, Double remainingSeconds) {
        this.weatherId = weatherId;
        this.remainingSeconds = remainingSeconds;
    }

    public boolean countDown(double seconds) {
        if (remainingSeconds == null) {
            return false;
        }
        remainingSeconds -= seconds;
        return remainingSeconds <= 0;
    }

    @Override
    public @Nullable CurrentWeatherComponent clone() {
        return new CurrentWeatherComponent(weatherId, remainingSeconds);
    }
}
