package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.plugins.auto.components.Standing;
import com.gaiagauntlet.gauntlet.plugins.auto.utils.MatchUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestate.constants.MatchState;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamListComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.ui.TeamUi;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The end screen: the winner banner and winning team's name over a podium of the top three. Shows
 * while the match is on its end screen, reading "NO WINNER" when the match ended without standings.
 */
public final class WinnerHud implements HudElement {

    private static final String[] PODIUM = {"#PodiumFirst", "#PodiumSecond", "#PodiumThird"};

    /** The standings the screen was built for, or null when it needs building. */
    @Nullable private List<Standing> built;

    @Nonnull @Override public String getId() {
        return "Winner";
    }

    @Nonnull @Override public String getMarkup() {
        return "Gauntlet/Games/EliminationZone/WinnerHud.ui";
    }

    @Override public int getOrder() {
        return 100;
    }

    @Override
    public boolean isVisible(@Nonnull PlayerRef player, @Nullable GameSession session) {
        return EZController.isEz(session) && MatchUtils.phase(session) == MatchState.ENDED;
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        cmd.set("#WinnerBanner.Background", "GG/WinnerBanner.png");
        built = null;
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        var match = MatchUtils.get(session);
        if (match == null || match.getStandings() == built) return;
        built = match.getStandings();

        var teams = TeamUi.teamsOf(session);
        var winner = built.isEmpty() ? null : teamOf(teams, built.getFirst());
        cmd.set("#WinnerName.Text", winner == null ? "NO WINNER" : TeamUi.displayName(winner).toUpperCase(Locale.ROOT));
        var icon = winner == null ? "" : winner.getUiIcon();
        cmd.set("#WinnerIconLeft.Visible", !icon.isBlank());
        cmd.set("#WinnerIconRight.Visible", !icon.isBlank());
        if (!icon.isBlank()) {
            cmd.set("#WinnerIconLeft.Background", icon);
            cmd.set("#WinnerIconRight.Background", icon);
        }

        for (int i = 0; i < PODIUM.length; i++) {
            var column = PODIUM[i];
            var standing = i < built.size() ? built.get(i) : null;
            var team = standing == null ? null : teamOf(teams, standing);
            cmd.set(column + ".Visible", standing != null);
            if (standing == null) continue;

            cmd.set(column + " #Name.Text", team == null ? standing.getTeamId() : TeamUi.displayName(team));
            cmd.set(column + " #Points.Text", "+" + standing.getPoints() + " pts");
            cmd.set(column + " #EventTotal.Text", standing.getEventTotal() + " event total");
            cmd.set(column + " #Initial.Visible", true);
            if (team != null) {
                TeamUi.icon(cmd, column + " #Icon", column + " #Initial", team);
            } else {
                cmd.set(column + " #Initial.Text", "?");
            }
        }
    }

    @Nullable
    private static TeamComponent teamOf(@Nullable TeamListComponent teams, @Nonnull Standing standing) {
        return teams == null ? null : teams.get(standing.getTeamId());
    }
}
