package com.gaiagauntlet.gauntlet.games.EliminationZone.scoring;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.components.EZGameComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZStates;
import com.gaiagauntlet.gauntlet.games.EliminationZone.ui.WinnerHud;
import com.gaiagauntlet.gauntlet.plugins.auto.components.Standing;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.TeamPlayerComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;

public final class EZScoring {

    private static final int KILLS_PER_POINT = 2;

    private EZScoring() {
    }

    public static void setup() {
        MatchEventRegistry.register(MatchStateEvent.class, EZController.ID, EZScoring::onMatchState);
    }

    private static void onMatchState(MatchStateEvent evt) {
        if (!Objects.equals(evt.getTo(), EZStates.ENDED.name()))
            return;
        var game = evt.getGame();
        game.ensure(EZGameComponent.getComponentType(), EZGameComponent::new).setStandings(rank(game));
        WinnerHud.update(game);
    }

    private record Entry(TeamComponent team, boolean alive, long eliminatedAt, int kills) {
    }

    @Nonnull
    public static List<Standing> rank(@Nonnull GameEcs game) {
        var teams = TeamUtils.teamsOf(game).orElse(null);
        if (teams == null)
            return List.of();
        var ledger = game.ensure(EZGameComponent.getComponentType(), EZGameComponent::new);
        var entries = new ArrayList<Entry>();
        for (var team : teams.getTeams().values()) {
            if (!team.isParticipant() || team.getSize() == 0)
                continue;
            var alive = false;
            long eliminatedAt = 0;
            var kills = 0;
            for (var uuid : team.getPlayers()) {
                var player = PlayerUtils.get(uuid);
                var teamPlayer = player == null ? null
                        : player.getComponentConcurrent(TeamPlayerComponent.getComponentType());
                if (teamPlayer != null)
                    kills += teamPlayer.getKills();
                if (ledger.isEliminated(uuid))
                    eliminatedAt = Math.max(eliminatedAt, ledger.eliminatedAt(uuid));
                else if (player != null && GameStore.isIn(player, game))
                    alive = true;
            }
            entries.add(new Entry(team, alive, eliminatedAt, kills));
        }
        entries.sort(Comparator.comparing(Entry::alive).reversed()
                .thenComparing(Comparator.comparingLong(Entry::eliminatedAt).reversed()));

        var table = EZGameConfig.get(game).getScoreTable();
        var standings = new ArrayList<Standing>();
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            var placement = table.length == 0 ? 0 : table[Math.min(i, table.length - 1)];
            var points = placement + entry.kills() / KILLS_PER_POINT;
            var total = entry.team().grantScore(points);
            standings.add(new Standing(entry.team().getId(), points, (int) Math.round(total)));
        }
        return standings;
    }
}
