package com.gaiagauntlet.gauntlet.plugins.config.components.assets;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.NotNull;

public class EmptyGameConfigAsset extends GameConfigAsset {
    public static final String ID = "Empty";

    public static final BuilderCodec<@NotNull GameConfigAsset> CODEC = BuilderCodec
        .builder(GameConfigAsset.class, GameConfigAsset::new, GameConfigAsset.ABSTRACT_CODEC)
        .build();
}
