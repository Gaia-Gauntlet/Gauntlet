package com.gaiagauntlet.gauntlet.plugins.teams.components;

import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamAsset;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public final class TeamGameComponent extends TeamComponent implements GameComponent {
    public TeamGameComponent(String id, @NonNull String name, @NonNull TeamType teamType, @NonNull UUID[] players, String icon) {
        super(id, name, teamType, players, icon);
    }

    public TeamGameComponent(String id, @Nullable String name, @Nullable TeamType teamType, int maxSize, @Nullable String icon) {
        super(id, name, teamType, maxSize, icon);
    }

    public TeamGameComponent(TeamAsset asset) {
        super(asset);
    }

    public Long getEliminatedAt() {
        long lastElim = 0;
        for (UUID player : players) {
            PlayerRef playerRef = Universe.get().getPlayer(player);
            if (Objects.isNull(playerRef)) continue;
            Ref<EntityStore> ref = playerRef.getReference();
            if (Objects.isNull(ref)) continue;
            var elim = ref.getStore().getComponent(ref, EliminatedComponent.getComponentType());
            if (Objects.isNull(elim)) return null;
            lastElim = Math.max(lastElim, elim.getEliminatedAt());
        }
        return lastElim;
    }

    public boolean isEliminated() {
        return Objects.nonNull(getEliminatedAt());
    }
}
