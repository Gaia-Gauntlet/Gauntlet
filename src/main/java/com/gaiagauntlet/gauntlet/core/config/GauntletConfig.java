package com.gaiagauntlet.gauntlet.core.config;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.riprod.configly.Config;
import com.riprod.configly.Configly;

import lombok.Getter;

public class GauntletConfig extends Config {
    @Nonnull
    public static final String TYPE = "GaiaGauntlet";

    @Nonnull
    public static final BuilderCodec<@NotNull GauntletConfig> CODEC = BuilderCodec
            .builder(GauntletConfig.class, GauntletConfig::new)
            .append(new KeyedCodec<>("HubId", Codec.STRING),
                    (config, v) -> config.hubId = v,
                    config -> config.getHubId())
            .documentation("The world name of the hub. Falls back to the universe default world")
            .add()
            .append(new KeyedCodec<>("TimeoutSeconds", Codec.LONG),
                    (config, v) -> config.timeoutSeconds = v,
                    config -> config.getTimeoutSeconds())
            .documentation("The amount of time allowed to pass before a player is kicked")
            .add()
            .append(new KeyedCodec<>("BatchSize", Codec.LONG),
                    (config, v) -> config.batchSize = v,
                    config -> config.getBatchSize())
            .documentation("How big the batches are")
            .add()
            .append(new KeyedCodec<>("BatchDelay", Codec.LONG),
                    (config, v) -> config.batchDelay = v,
                    config -> config.getBatchDelay())
            .documentation("How long the delay is between batches")
            .add()
            .build();

    @Nonnull
    public static GauntletConfig get() {
        return Configly.getOrElse(TYPE, GauntletConfig.class, new GauntletConfig());
    }

    @Getter
    private Long timeoutSeconds = 180L;
    @Getter
    private Long batchSize = 10L;
    @Getter
    private Long batchDelay = 5L;
    @Getter
    private String hubId = null;
}
