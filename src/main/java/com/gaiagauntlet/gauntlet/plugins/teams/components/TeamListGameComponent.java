package com.gaiagauntlet.gauntlet.plugins.teams.components;

import com.gaiagauntlet.gauntlet.core.gamestore.components.GameComponent;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

public final class TeamListGameComponent extends TeamListComponent implements GameComponent {
    @Nonnull public static final BuilderCodec<TeamListGameComponent> CODEC = AssetBuilderCodec
        .builder(
            TeamListGameComponent.class,
            TeamListGameComponent::new
        )
        .append(new KeyedCodec<>("TeamList", new MapCodec<>(TeamComponent.CODEC, ConcurrentHashMap::new)),
            (team, v) -> team.teamList = v,
            team -> team.teamList)
        .documentation("The full list of teams in this game.")
        .add()
        .build();
}
