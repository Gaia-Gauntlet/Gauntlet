package com.gaiagauntlet.gauntlet.games.EliminationZone.weather.events;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZState;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZStates;
import com.gaiagauntlet.gauntlet.games.EliminationZone.weather.utils.WeatherUtils;
import com.gaiagauntlet.gauntlet.plugins.events.assets.GameEventConfig;
import com.gaiagauntlet.gauntlet.plugins.events.events.SequenceEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;

import lombok.Getter;

import org.jetbrains.annotations.NotNull;

/** Typed weather event fields. */
@Getter
public final class WeatherEventConfig extends GameEventConfig {
    public static final String Id = "WeatherEvent";

    public static final BuilderCodec<@NotNull WeatherEventConfig> CODEC = BuilderCodec
            .builder(WeatherEventConfig.class, WeatherEventConfig::new, BASE_CODEC)
            .append(new KeyedCodec<>("WeatherEventId", Codec.STRING),
                    (event, value) -> event.weatherId = value,
                    event -> event.weatherId)
            .documentation("The weather to force when the event is not random.")
            .add()
            .append(new KeyedCodec<>("IsRandom", Codec.BOOLEAN),
                    (event, value) -> event.isRandom = value != null && value,
                    event -> event.isRandom)
            .documentation("Whether or not a random weather event should be run.")
            .add()
            .append(new KeyedCodec<Double>("DurationSeconds", Codec.DOUBLE),
                    (event, value) -> event.durationSeconds = value == null ? 0.0 : value,
                    event -> event.durationSeconds)
            .add()
            .build();

    private boolean isRandom = false;
    private double durationSeconds;
    private String weatherId = null;

    public WeatherEventConfig() {
    }

    public WeatherEventConfig(String weatherId) {
        this.isRandom = weatherId == null || weatherId.isBlank();
        this.weatherId = isRandom ? null : weatherId;
    }

    @Override
    public String getId() {
        return Id;
    }

    @Override
    public String describe() {
        return isRandom ? "Random weather" : "Weather " + weatherId;
    }

    public static void execute(SequenceEvent gameEvent) {
        if (!(gameEvent.getConfig() instanceof WeatherEventConfig eventConfig)) {
            HytaleLogger.getLogger().atWarning().log("Weather event emitted without passing the weather class!");
            return;
        }

        var store = gameEvent.getStore();
        if (EZState.currentState(store) == EZStates.SUDDEN_DEATH)
            return;

        var world = GameStore.getContext(store).getWorld();
        GauntletUtils.run(world, () -> {
            if (eventConfig.isRandom()) {
                WeatherUtils.doRandomWeather(store, eventConfig.getDurationSeconds());
                return;
            }
            WeatherUtils.doWeather(store, eventConfig.getWeatherId(), eventConfig.getDurationSeconds());
        });
    }

}
