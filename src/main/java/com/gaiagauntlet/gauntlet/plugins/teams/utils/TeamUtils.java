package com.gaiagauntlet.gauntlet.plugins.teams.utils;

import com.gaiagauntlet.gauntlet.core.party.components.PartyComponent;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamListComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamListAsset;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.TeamPlayerComponent;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.*;
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
        return GameStore.ensureStore(world, session).ensure(TeamListComponent.getGameComponentType(), TeamListComponent::new);
    }
    /** Creates a new team component if it is missing */
    @Nonnull
    public static TeamListComponent withTeamList(ComponentAccessor<EntityStore> accessor, String sessionId) {
        return GameStore.ensureStore(accessor, sessionId).ensure(TeamListComponent.getGameComponentType(), TeamListComponent::new);
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
        return playerComponent.getKills();
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

    /**
     * Initialise teams to a team list component from an asset. Does not populate teams beyond what
     * has been manually defined in the {@code TeamListAsset}
     */
    public static void initialiseTeams(TeamListComponent teams, TeamListAsset teamAsset) {
        for (TeamComponent team : teamAsset.getTeamList().values()) {
            teams.addTeam(team.getId(), team.clone());
        }
    }

    /**
     * Distribute players across existing team components. Any players already in a team remain
     * there, and new players are fit in appropriately
     */
    public static void distributePlayers(Collection<PlayerRef> players, TeamListComponent teams) {
        var unassigned = new LinkedHashSet<>(players);
        // First pass - Clean offline players, fill from parties if enabled.
        for (var team : teams.getTeams().values()) {
            for (UUID uuid : team.getPlayers()) {
                // Remove offline players from team
                PlayerRef player = PlayerUtils.get(uuid);
                if (Objects.isNull(player)) {
                    team.remove(uuid);
                    continue;
                }
                // Remove players already assigned to team from unassigned
                unassigned.remove(player);
            }

            if (!teams.isRespectParties()) continue;

            // Check if any existing parties will fit in this team.
            for (PartyComponent party : PartyUtils.getParties()) {
                if (team.getSize() + party.getAllOnlinePlayers().size() <= teams.getTeamSize()) {
                    // Party fits, add all players to the team
                    for (PlayerRef player : party.getAllOnlinePlayers()) {
                        // Don't include players not requested to be distributed
                        if (!unassigned.contains(player)) continue;
                        party.addPlayer(player.getUuid());
                        unassigned.remove(player);
                    }
                }
            }
        }

        // Second pass - Fill gaps with players not yet assigned (and not in a party if enabled)
        for (var team : teams.getTeams().values()) {
            while (team.getSize() < teams.getTeamSize() && !unassigned.isEmpty()) {
                team.add(unassigned.removeFirst().getUuid());
            }
            if (unassigned.isEmpty()) break;
        }
    }
}
