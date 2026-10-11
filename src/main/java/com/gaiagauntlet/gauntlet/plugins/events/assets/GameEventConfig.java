package com.gaiagauntlet.gauntlet.plugins.events.assets;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@Getter
public abstract class GameEventConfig {

    @Nonnull
    public static final CodecMapCodec<GameEventConfig> CODEC = new CodecMapCodec<>("Type");

    @Nonnull
    public static final BuilderCodec<@NotNull GameEventConfig> BASE_CODEC = BuilderCodec
        .abstractBuilder(GameEventConfig.class)
        .build();

    private double atMatchSeconds;

    protected GameEventConfig() {
    }

    protected GameEventConfig(double atMatchSeconds) {
        this.atMatchSeconds = atMatchSeconds;
    }

    public static <T extends GameEventConfig> void register(String id, Class<T> handlerClass, Codec<T> handlerCodec) {
        CODEC.register(id, handlerClass, handlerCodec);
    }

    public abstract String getId();

    @Nonnull
    public String describe() {
        return getId();
    }
}
