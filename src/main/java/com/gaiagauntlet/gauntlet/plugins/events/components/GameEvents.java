package com.gaiagauntlet.gauntlet.plugins.events.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.Float2ObjectMapCodec;
import it.unimi.dsi.fastutil.floats.Float2ObjectMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Gave Event Configurations - intended to exist inside of a different game's
 * config
 */
public final class GameEvents {
    @Nonnull
    public static final BuilderCodec<@NotNull GameEvents> CODEC = BuilderCodec
            .builder(GameEvents.class, GameEvents::new)
            .append(new KeyedCodec<Double>("IntervalSeconds", Codec.DOUBLE),
                    (p, v) -> p.intervalSeconds = v == null ? 0.0 : v, p -> p.intervalSeconds)
            .add()
            .append(new KeyedCodec<Double>("FirstDrawAtSeconds", Codec.DOUBLE),
                    (p, v) -> p.firstDrawAtSeconds = v == null ? 0.0 : v, p -> p.firstDrawAtSeconds)
            .add()
            .append(new KeyedCodec<>("Entries",
                    new ArrayCodec<>(GameEventConfig.CODEC, GameEventConfig[]::new)),
                    (p, v) -> p.entries = v,
                    p -> p.entries)
            .add()
            .append(
                    new KeyedCodec<>("Scheduled",
                            new Float2ObjectMapCodec<>(GameEventConfig.CODEC,
                                    Float2ObjectOpenHashMap::new)),
                    (evt, s) -> evt.scheduled = s,
                    evt -> evt.scheduled)
            .add()
            .afterDecode(interaction -> {
                if (interaction.scheduled != null) {
                    interaction.sortedKeys = interaction.scheduled.keySet().toFloatArray();
                    Arrays.sort(interaction.sortedKeys);
                }
            })
            .build();

    @Getter private double intervalSeconds = 0.0;
    @Getter private double firstDrawAtSeconds = 0.0;
    @Getter private GameEventConfig[] entries;
    @Getter private Float2ObjectMap<GameEventConfig> scheduled;
    @Getter
    protected float[] sortedKeys;

    public GameEvents() {
    }
}
