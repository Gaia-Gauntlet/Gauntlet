package com.gaiagauntlet.gauntlet.games.EliminationZone.weather.utils;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.weather.components.CurrentWeatherComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.weather.components.WeatherPoolOptionComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.builtin.weather.resources.WeatherResource;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class WeatherUtils {

    public static final String DEFAULT_POOL_ID = "Standard";
    public static final String SUDDEN_DEATH_WEATHER = "GG_Sudden_Death";

    @Nonnull
    private static final Random random = new Random();

    private WeatherUtils() {
    }

    public static Optional<CurrentWeatherComponent> current(@Nullable GameEcs gameStore) {
        return gameStore == null ? Optional.empty() : gameStore.get(CurrentWeatherComponent.getComponentType());
    }

    @Nonnull
    public static List<String> weatherIds(@Nullable GameEcs gameStore) {
        var ids = new LinkedHashSet<String>();
        if (gameStore != null) {
            for (var option : EZGameConfig.get(gameStore).getWeatherPoolOptions()) {
                ids.add(option.getWeather());
            }
        }
        var all = new ArrayList<>(Weather.getAssetMap().getAssetMap().keySet());
        all.sort(String::compareTo);
        ids.addAll(all);
        return List.copyOf(ids);
    }

    public static void doWeather(GameEcs gameStore, @Nonnull String weatherId, @Nullable Double durationSeconds) {
        if (Weather.getAssetMap().getAsset(weatherId) == null) {
            throw new IllegalArgumentException("Unknown weather '" + weatherId + "'");
        }
        var option = poolOption(gameStore, weatherId);
        Double seconds = durationSeconds;
        if (seconds != null && seconds <= 0) {
            seconds = option == null ? null : rollDuration(option);
        }
        apply(gameStore, weatherId, seconds);
    }

    @Nonnull
    public static String doRandomWeather(GameEcs gameStore, @Nullable Double durationSeconds) {
        WeatherPoolOptionComponent[] pool = EZGameConfig.get(gameStore).getWeatherPoolOptions();
        if (pool == null || pool.length == 0) {
            throw new IllegalStateException("No weather options configured, cannot pick random weather");
        }
        var chosen = pickWeighted(pool, random);
        apply(gameStore, chosen.getWeather(),
                durationSeconds != null && durationSeconds > 0 ? durationSeconds : rollDuration(chosen));
        return chosen.getWeather();
    }

    public static void doStop(GameEcs gameStore) {
        apply(gameStore, null, null);
    }

    public static void doSuddenDeathWeather(GameEcs gameStore) {
        apply(gameStore, SUDDEN_DEATH_WEATHER, null);
    }

    private static void apply(GameEcs gameStore, @Nullable String weatherAssetId, @Nullable Double durationSeconds) {
        var world = GameStore.getContext(gameStore).getWorld();
        if (world == null) {
            throw new IllegalStateException("No active gameplay world; cannot change weather");
        }
        var resource = world.getEntityStore().getStore().getResource(WeatherResource.getResourceType());
        if (resource == null) {
            throw new IllegalStateException("World '" + world.getName() + "' has no WeatherResource (is the Weather plugin loaded?)");
        }
        resource.setForcedWeather(weatherAssetId);
        var currentWeatherComponent = gameStore.ensure(CurrentWeatherComponent.getComponentType(),
                CurrentWeatherComponent::new);
        currentWeatherComponent.setWeatherId(weatherAssetId);
        currentWeatherComponent.setRemainingSeconds(durationSeconds);
    }

    @Nullable
    private static WeatherPoolOptionComponent poolOption(GameEcs gameStore, @Nonnull String weatherId) {
        for (var option : EZGameConfig.get(gameStore).getWeatherPoolOptions()) {
            if (option.getWeather().equals(weatherId)) {
                return option;
            }
        }
        return null;
    }

    private static double rollDuration(@Nonnull WeatherPoolOptionComponent option) {
        return rollRange(option.getMinDurationSeconds(), option.getMaxDurationSeconds(), random);
    }

    private static double rollRange(double first, double second, @Nonnull Random random) {
        double min = Math.min(first, second);
        double max = Math.max(first, second);
        if (!Double.isFinite(min) || !Double.isFinite(max))
            return min;
        if (min == max)
            return min;
        return min + random.nextDouble() * (max - min);
    }

    @Nonnull
    private static WeatherPoolOptionComponent pickWeighted(@Nonnull WeatherPoolOptionComponent[] options,
            @Nonnull Random random) {
        double[] weights = new double[options.length];
        double total = 0;
        for (int i = 0; i < options.length; i++) {
            WeatherPoolOptionComponent option = options[i];
            double rolled = rollRange(option.getMinWeight(), option.getMaxWeight(), random);
            weights[i] = Double.isFinite(rolled) ? Math.max(0.0, rolled) : 0.0;
            total += weights[i];
        }
        if (total <= 0) {
            return options[random.nextInt(options.length)];
        }
        double roll = random.nextDouble() * total;
        double accumulated = 0;
        for (int i = 0; i < options.length; i++) {
            accumulated += weights[i];
            if (roll < accumulated) {
                return options[i];
            }
        }
        return options[options.length - 1];
    }
}
