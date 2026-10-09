package com.gaiagauntlet.gauntlet.plugins.auto.components;

import org.jetbrains.annotations.NotNull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import lombok.Getter;

/** One team's result in a finished match: the points it earned and its event total afterwards. */
public final class Standing {

    public static final BuilderCodec<@NotNull Standing> CODEC = BuilderCodec.builder(Standing.class, Standing::new)
            .append(new KeyedCodec<>("Team", Codec.STRING), (s, v) -> s.teamId = v, s -> s.teamId)
            .add()
            .append(new KeyedCodec<>("Points", Codec.INTEGER), (s, v) -> s.points = v == null ? 0 : v, s -> s.points)
            .add()
            .append(new KeyedCodec<>("EventTotal", Codec.INTEGER), (s, v) -> s.eventTotal = v == null ? 0 : v, s -> s.eventTotal)
            .add()
            .build();

    @Getter private String teamId = "";
    @Getter private int points;
    @Getter private int eventTotal;

    private Standing() {
    }

    public Standing(@NotNull String teamId, int points, int eventTotal) {
        this.teamId = teamId;
        this.points = points;
        this.eventTotal = eventTotal;
    }
}
