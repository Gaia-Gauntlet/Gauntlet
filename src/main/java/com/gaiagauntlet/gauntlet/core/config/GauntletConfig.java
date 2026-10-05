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
            .build();

    @Nonnull
    public static GauntletConfig get() {
        return Configly.getOrElse(TYPE, GauntletConfig.class, new GauntletConfig());
    }

    @Getter
    private String hubId = null;
}
