package com.gaiagauntlet.gauntlet.plugins.career.components;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.EnumMapCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import lombok.Getter;
import lombok.Setter;

public class CareerComponent implements Component<EntityStore> {
    public static final String ID = "Career";
    @Getter
    @Setter
    private static ComponentType<EntityStore, CareerComponent> componentType;

    private static final Codec<Map<String, Map<String, Long>>> GAME_STATS_CODEC = new MapCodec<>(
            new MapCodec<Long, HashMap<String, Long>>(Codec.LONG, HashMap::new, false),
            HashMap::new,
            false);

    public static final BuilderCodec<@NotNull CareerComponent> CODEC = BuilderCodec.builder(CareerComponent.class,
            CareerComponent::new)
            .append(new KeyedCodec<>("UniversalStats",
                    new EnumMapCodec<>(CareerStats.class, Codec.LONG, false)),
                    (config, value) -> config.universalStats = value,
                    config -> config.universalStats)
            .add()
            .append(new KeyedCodec<>("GameStats", GAME_STATS_CODEC),
                    (config, value) -> config.gameStats = value,
                    config -> config.gameStats)
            .add()
            .build();

    private Map<@NotNull CareerStats, Long> universalStats;
    private Map<String, Map<String, Long>> gameStats;

    public CareerComponent(CareerComponent other) {
        this.universalStats = new EnumMap<>(other.universalStats);
        this.gameStats = new HashMap<>();
        other.gameStats.forEach((gameId, stats) -> this.gameStats.put(gameId, new HashMap<>(stats)));
    }

    public CareerComponent() {
        this.universalStats = new EnumMap<>(CareerStats.class);
        this.gameStats = new HashMap<>();
    }

    public CareerComponent clone() {
        return new CareerComponent(this);
    }

    public Long get(String gameId, String statId) {
        return get(gameId).computeIfAbsent(statId, stat -> 0L);
    }

    public Long increment(String gameId, String statId, long amount) {
        long value = get(gameId, statId) + amount;
        get(gameId).put(statId, value);
        return value;
    }

    public Map<String, Long> get(String gameId) {
        return gameStats.computeIfAbsent(gameId, stat -> new HashMap<>());
    }

    public Long get(@NotNull CareerStats statId) {
        return universalStats.computeIfAbsent(statId, stat -> 0L);
    }

    public Long increment(@NotNull CareerStats statId, long amount) {
        long value = get(statId) + amount;
        universalStats.put(statId, value);
        return value;
    }
}
