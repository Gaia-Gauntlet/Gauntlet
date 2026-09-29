package com.gaiagauntlet.gauntlet.plugins.teams.components;

import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamAsset;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class TeamSessionComponent extends TeamComponent implements SessionComponent {
    public TeamSessionComponent(String id, @NonNull String name, @NonNull TeamType teamType, @NonNull UUID[] players, String icon) {
        super(id, name, teamType, players, icon);
    }

    public TeamSessionComponent(String id, @Nullable String name, @Nullable TeamType teamType, int maxSize, @Nullable String icon) {
        super(id, name, teamType, maxSize, icon);
    }

    public TeamSessionComponent(TeamAsset asset) {
        super(asset);
    }
}
