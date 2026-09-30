package com.gaiagauntlet.gauntlet.plugins.teams.components;

import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class TeamListComponent {
    @Nonnull Map<String, TeamComponent> teamList = new ConcurrentHashMap<>();
    @Getter @Nonnull Map<UUID, TeamComponent> uuidToTeam = new ConcurrentHashMap<>();

    // Teams

    // TODO: Test these methods to ensure the cached uuidToTeam remains stable as intended.

    public boolean addTeam(TeamComponent teamComponent) {
        // Assert that new team id is unique.
        if (isTeamExist(teamComponent.getId())) return false;
        teamList.put(teamComponent.getId(), teamComponent);
        // Attempt to add existing players on component to cache. Most of the time this will not
        // be used since the team component passed in should be fresh (i.e. no players on it)
        for (UUID player : teamComponent.getPlayers()) {
            boolean result = addPlayerToTeam(teamComponent.getId(), player);
            // If a player was unable to be added, check if it's because they're already cached as
            // on this team, and if not, remove them from this new team in favour of the
            // pre-existing team.
            if (!result
                && !getTeamForPlayer(player).getId().equals(teamComponent.getId())
            ) teamComponent.remove(player);
            uuidToTeam.put(player, teamComponent);
        }
        return true;
    }

    public boolean removeTeam(String teamId) {
        TeamComponent team = getTeam(teamId);
        if (Objects.isNull(team)) return false;

        // Clear cached player teams for removed team
        for (UUID player : team.getPlayers()) {
            removePlayerFromTeam(teamId, player);
        }
        teamList.remove(teamId);
        return true;
    }

    public TeamComponent getTeam(String teamId) {
        return teamList.get(teamId);
    }

    public boolean isTeamExist(String teamId) {
        return Objects.nonNull(getTeam(teamId));
    }

    public Collection<TeamComponent> getTeams() {
        return teamList.values();
    }

    // Players

    public boolean addPlayerToTeam(String teamId, UUID player) {
        if (isPlayerOnTeam(player)) return false;
        var team = getTeam(teamId);
        if (Objects.isNull(team)) return false;
        team.add(player);
        getUuidToTeam().put(player, team);
        return true;
    }

    public boolean removePlayerFromTeam(String teamId, UUID player) {
        var team = getTeam(teamId);
        if (Objects.isNull(team)) return false;
        if (!team.remove(player)) return false;
        getUuidToTeam().remove(player);
        return true;
    }

    public TeamComponent getTeamForPlayer(UUID player) {
        return uuidToTeam.get(player);
    }

    public boolean isPlayerOnTeam(UUID player) {
        return getPlayers().contains(player);
    }

    public Collection<UUID> getPlayers() {
        return uuidToTeam.keySet();
    }
}
