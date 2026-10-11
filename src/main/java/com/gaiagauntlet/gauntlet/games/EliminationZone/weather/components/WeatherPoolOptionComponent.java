package com.gaiagauntlet.gauntlet.games.EliminationZone.weather.components;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class WeatherPoolOptionComponent {

    @Nonnull
    public static final BuilderCodec<@NotNull WeatherPoolOptionComponent> CODEC = BuilderCodec
            .builder(WeatherPoolOptionComponent.class, WeatherPoolOptionComponent::new)

            .append(new KeyedCodec<String>("Weather", Codec.STRING),
                    (o, v) -> {
                        if (v != null)
                            o.weather = v;
                    },
                    WeatherPoolOptionComponent::getWeather)
            .addValidatorLate(() -> optionalAssetKey(Weather::getAssetStore).late())
            .add()
            .append(new KeyedCodec<Double>("MinDurationSeconds", Codec.DOUBLE),
                    (o, v) -> {
                        if (v != null)
                            o.minDurationSeconds = v;
                    },
                    WeatherPoolOptionComponent::getMinDurationSeconds)
            .add()
            .append(new KeyedCodec<Double>("MaxDurationSeconds", Codec.DOUBLE),
                    (o, v) -> {
                        if (v != null)
                            o.maxDurationSeconds = v;
                    },
                    WeatherPoolOptionComponent::getMaxDurationSeconds)
            .add()
            .append(new KeyedCodec<Double>("MinWeight", Codec.DOUBLE),
                    (o, v) -> {
                        if (v != null)
                            o.minWeight = v;
                    },
                    WeatherPoolOptionComponent::getMinWeight)
            .add()
            .append(new KeyedCodec<Double>("MaxWeight", Codec.DOUBLE),
                    (o, v) -> {
                        if (v != null)
                            o.maxWeight = v;
                    },
                    WeatherPoolOptionComponent::getMaxWeight)
            .add()
            .build();

    @Getter
    @Nonnull
    private String weather = "";
    @Getter
    private double minDurationSeconds = 60.0;
    @Getter
    private double maxDurationSeconds = 60.0;
    @Getter
    private double minWeight = 1.0;
    @Getter
    private double maxWeight = 1.0;

    public WeatherPoolOptionComponent() {
    }

    /**
     * Convenience constructor for a fixed duration and weight.
     */
    public WeatherPoolOptionComponent(@Nonnull String weather, double durationSeconds, double weight) {
        this(weather, durationSeconds, durationSeconds, weight, weight);
    }

    public WeatherPoolOptionComponent(@Nonnull String weather,
            double minDurationSeconds, double maxDurationSeconds, double minWeight, double maxWeight) {
        this.weather = weather;
        this.minDurationSeconds = minDurationSeconds;
        this.maxDurationSeconds = maxDurationSeconds;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
    }

    private static <K> Validator<K> optionalAssetKey(Supplier<? extends AssetStore<K, ?, ?>> supplier) {
        return new Validator<>() {
            @Override
            public void accept(K value, ValidationResults results) {
                AssetStore<K, ?, ?> store = supplier.get();
                if (store != null && value != null
                        && (!(value instanceof String text) || !text.isBlank())) {
                    new AssetKeyValidator<K>(() -> store).accept(value, results);
                }
            }

            @Override
            public void updateSchema(SchemaContext context, Schema schema) {
                AssetStore<K, ?, ?> store = supplier.get();
                if (store != null) {
                    new AssetKeyValidator<K>(() -> store).updateSchema(context, schema);
                }
            }
        };
    }

    @Nullable
    public static WeatherPoolOptionComponent[] resolvePool(GameEcs gameStore) {
        return EZGameConfig.get(gameStore).getWeatherPoolOptions();
    }
}
