package com.gaiagauntlet.gauntlet.plugins.teams.components.game;

import com.gaiagauntlet.gauntlet.core.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamType;
import com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamListAsset;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;

public final class TeamListGameComponent implements GameComponent {
    
    @Nonnull
    private Map<String, TeamComponent> teamList;

}
