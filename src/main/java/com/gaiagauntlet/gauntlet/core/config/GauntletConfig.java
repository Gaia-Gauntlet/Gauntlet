package com.gaiagauntlet.gauntlet.core.config;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.riprod.configly.Config;
import com.riprod.configly.Configly;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.concurrent.TimeUnit;

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
            .append(new KeyedCodec<>("TeamsPreset", Codec.STRING),
                    (config, v) -> config.teamsPreset = v,
                    config -> config.getTeamsPreset())
            .documentation("The team preset a session plays with when an admin has not loaded one for it")
            .add()
            .build();

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final GauntletConfig DEFAULTS = new GauntletConfig();

    @Nonnull
    public static GauntletConfig get() {
        var config = Configly.get(TYPE, GauntletConfig.class);
        if (config != null)
            return config;
        var stored = Configly.get(TYPE, Config.class);
        LOGGER.atWarning().atMostEvery(60, TimeUnit.SECONDS).log("%s config is not loaded, using defaults (stored: %s)",
                TYPE, stored == null ? "nothing" : stored.getClass().getName() + " from " + stored.getClass().getClassLoader());
        return DEFAULTS;
    }

    @Getter
    private Long timeoutSeconds = 180L;
    @Getter
    private Long batchSize = 10L;
    @Getter
    private Long batchDelay = 5L;
    @Getter
    private String hubId = null;
    @Getter
    private String teamsPreset = "EZDefault";
}
