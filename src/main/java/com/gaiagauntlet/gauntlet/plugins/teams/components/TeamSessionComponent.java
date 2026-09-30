package com.gaiagauntlet.gauntlet.plugins.teams.components;

import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamAsset;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class TeamSessionComponent extends TeamComponent implements SessionComponent {
    public static BuilderCodec<TeamSessionComponent> CODEC = BuilderCodec
        .builder(TeamSessionComponent.class, TeamSessionComponent::new)
        .append(new KeyedCodec<>("Name", Codec.STRING),
            (team, v) -> team.name = v == null ? "" : v,
            team -> team.name)
        .documentation("The display name shown to players. Defaults to the asset id when blank.")
        .add()
        .append(new KeyedCodec<>("TeamType", new EnumCodec<>(TeamType.class)),
            (team, v) -> team.teamType = v,
            team -> team.teamType)
        .documentation("The display name shown to players. Defaults to the asset id when blank.")
        .add()
        .append(new KeyedCodec<>("Icon", Codec.STRING),
            (team, v) -> team.icon = v == null ? "" : v,
            team -> team.icon)
        .addValidator(new CommonAssetValidator("png", "UI/Custom/"))
        .documentation("UI asset team icon, for example GG/TeamIcons/Tricky_Trorks.png. Optional.")
        .add()
        .append(new KeyedCodec<>("Players", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new)),
            (team, v) -> team.players = v,
            team -> team.players)
        .documentation("The team roster")
        .add()
        .build();

    public TeamSessionComponent(String id, @NonNull String name, @NonNull TeamType teamType, @NonNull UUID[] players, String icon) {
        super(id, name, teamType, players, icon);
    }

    public TeamSessionComponent(String id, @Nullable String name, @Nullable TeamType teamType, int maxSize, @Nullable String icon) {
        super(id, name, teamType, maxSize, icon);
    }

    public TeamSessionComponent(TeamAsset asset) {
        super(asset);
    }

    private TeamSessionComponent() {
        super();
    }
}
