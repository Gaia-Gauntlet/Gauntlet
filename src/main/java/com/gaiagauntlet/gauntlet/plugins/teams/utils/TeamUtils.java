package com.gaiagauntlet.gauntlet.plugins.teams.utils;

import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamListComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamListComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamListAsset;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.TeamPlayerComponent;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TeamUtils {
    // Static utils for getting stuff about a team / list of teams

    public static boolean hasOnline(TeamComponent team) {
        for (UUID uuid : team.getPlayers()) {
            var playerRef = Universe.get().getPlayer(uuid);
            if (Objects.nonNull(playerRef)) return true;
        }
        return false;
    }

    /** Creates a new team component if it is missing */
    @Nonnull
    public static TeamListComponent withTeamList(World world, String session) {
        return GameStore.withStore(world, session).ensure(TeamListComponent.getGameComponentType(), TeamListComponent::new);
    }
    /** Creates a new team component if it is missing */
    @Nonnull
    public static TeamListComponent withTeamList(ComponentAccessor<EntityStore> accessor, String sessionId) {
        return GameStore.withStore(accessor, sessionId).ensure(TeamListComponent.getGameComponentType(), TeamListComponent::new);
    }
    /** Creates a new team list if it is missing */
    @Nullable
    public static TeamComponent withTeam(World world, String sessionId, String teamId) {
        return withTeamList(world, sessionId).get(teamId);
    }
    /** Creates a new team list if it is missing */
    @Nullable
    public static TeamComponent withTeam(ComponentAccessor<EntityStore> accessor, String sessionId, String teamId) {
        return withTeamList(accessor, sessionId).get(teamId);
    }

    /**
     * Checks if a team has any alive players. Truthy if and only if a player on
     * this team is online and is not eliminated.
     */
    public static boolean hasAlive(TeamComponent team) {
        for (UUID uuid : team.getPlayers()) {
            var playerRef = Universe.get().getPlayer(uuid);
            if (Objects.isNull(playerRef)) continue;
            var ref = playerRef.getReference();
            assert ref != null;
            var eliminated = ref.getStore().getComponent(ref, EliminatedComponent.getComponentType());
            if (Objects.isNull(eliminated)) return true;
        }
        return false;
    }

    public static Collection<PlayerRef> getOnlinePlayers(TeamListComponent teams) {
        var onlinePlayers = new HashSet<PlayerRef>();
        for (UUID player : teams.getPlayers()) {
            PlayerRef playerRef = Universe.get().getPlayer(player);
            if (Objects.nonNull(playerRef)) onlinePlayers.add(playerRef);
        }
        return onlinePlayers;
    }

    public static Collection<PlayerRef> getOnlinePlayers(TeamComponent team) {
        var onlinePlayers = new HashSet<PlayerRef>();
        for (UUID player : team.getPlayers()) {
            PlayerRef playerRef = Universe.get().getPlayer(player);
            if (Objects.nonNull(playerRef)) onlinePlayers.add(playerRef);
        }
        return onlinePlayers;
    }

    public static TeamListComponent from(TeamListAsset asset) {
        var teamSession = new TeamListComponent();

        for (var team : asset.getTeamList().entrySet()) {
            teamSession.addTeam(team.getKey(), team.getValue().clone());
        }

        return teamSession;
    }

    public static double getScore(PlayerRef player) {
        if (player == null)
            return 0;
        var playerComponent = player.getComponentConcurrent(TeamPlayerComponent.getComponentType());
        if (playerComponent == null)
            return 0;
        return playerComponent.getScore();
    }

    public static double getScore(UUID playerId) {
        return getScore(PlayerUtils.get(playerId));
    }

    public static double getScore(TeamListComponent team, String teamId) {
        if (team == null || teamId == null)
            return 0d;
        return getScore(team.get(teamId));
    }

    public static double getScore(TeamComponent team) {
        if (team == null)
            return 0d;
        return team.getPlayers().stream().mapToDouble(TeamUtils::getScore).sum();
    }

    public static double getScore(TeamListComponent teams) {
        if (teams == null) return 0d;
        return teams.getTeams().values().stream()
            .mapToDouble(TeamUtils::getScore).sum();
    }
}
