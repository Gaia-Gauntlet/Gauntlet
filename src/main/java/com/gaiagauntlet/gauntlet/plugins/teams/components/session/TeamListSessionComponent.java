package com.gaiagauntlet.gauntlet.plugins.teams.components.session;

import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TeamListSessionComponent implements SessionComponent {
    @Nonnull
    public static final BuilderCodec<TeamListSessionComponent> CODEC = AssetBuilderCodec
        .builder(
            TeamListSessionComponent.class,
            TeamListSessionComponent::new
        )
        .append(new KeyedCodec<>("TeamList", new MapCodec<>(TeamComponent.CODEC, ConcurrentHashMap::new)),
            (team, v) -> team.teamList = v,
            team -> team.teamList)
        .documentation("The full list of teams in this session.")
        .add()
        .build();

    @Nonnull @Getter private Map<String, TeamComponent> teamList = new ConcurrentHashMap<>();

    public TeamComponent getTeam(String teamId) {
        return teamList.get(teamId);
    }

    // add more here, since this is not enough
}
