package com.gaiagauntlet.gauntlet.plugins.config.components.assets;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.NotNull;

public class EmptyGameConfig extends GameConfig {
    public static final String ID = "Empty";

    public static final BuilderCodec<@NotNull GameConfig> CODEC = BuilderCodec
        .builder(GameConfig.class, GameConfig::new, GameConfig.ABSTRACT_CODEC)
        .build();
}
