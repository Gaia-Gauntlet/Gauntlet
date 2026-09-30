package com.gaiagauntlet.gauntlet.plugins.teams.utils;

import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamListAsset;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.session.TeamListSessionComponent;
import com.hypixel.hytale.server.core.universe.Universe;

import java.util.Objects;
import java.util.UUID;

public class TeamUtils {
    // Static utils for getting stuff about a team / list of teams

    public static boolean hasOnline(TeamComponent team) {
        for (UUID uuid : team.getPlayers()) {
            var playerRef = Universe.get().getPlayer(uuid);
            if (Objects.nonNull(playerRef)) return true;
        }
        return false;
    }

    /**
     * Checks if a team has any alive players. Truthy if and only if a player on this team is online
     * and is not eliminated.
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

    public static void hydrateSessionFromAssets(TeamListSessionComponent teamListSessionComponent, TeamListAsset teamListAsset) {
        // TODO: Finish impl
        var sessionsTeams = teamListSessionComponent.getTeamList();
        for (TeamComponent team : teamListAsset.getTeamList().values()) {
            sessionsTeams.computeIfAbsent(team.getId(), _ -> team.clone());
        }
    }
}
