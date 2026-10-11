package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.hud.events.HudEvent;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudDataSnapshot;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.components.EZGameComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZState;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZStates;
import com.gaiagauntlet.gauntlet.plugins.auto.components.Standing;
import com.gaiagauntlet.gauntlet.plugins.gamestate.utils.MatchUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamListComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The end screen: the winner banner and winning team's name over a podium of
 * the top three. Shows
 * while the match is on its end screen, reading "NO WINNER" when the match
 * ended without standings.
 */
public final class WinnerHud implements HudElement {

    public static final String ID = "Winner";

    private static final String[] PODIUM = { "#PodiumFirst", "#PodiumSecond", "#PodiumThird" };

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    @Nonnull
    @Override
    public String getMarkup() {
        return "Gauntlet/Games/EliminationZone/WinnerHud.ui";
    }

    @Override
    public int getOrder() {
        return 100;
    }

    public record WinnerHudSnapshot(
            GameEcs game,
            List<Standing> standings,
            @Nullable TeamListComponent teams) implements HudDataSnapshot {
    }

    @Nullable
    public static WinnerHudSnapshot snapshot(@Nullable GameEcs game) {
        if (game == null || EZState.currentState(game) != EZStates.ENDED)
            return null;
        var standings = game.get(EZGameComponent.getComponentType()).map(EZGameComponent::getStandings).orElse(List.of());
        if (standings.isEmpty())
            return null;
        return new WinnerHudSnapshot(game, standings, TeamUtils.teamsOf(game).orElse(null));
    }

    public static void update(@Nonnull GameEcs game) {
        new HudEvent(game.getSessionId(), ID, snapshot(game)).dispatch();
    }

    @Override
    public WinnerHudSnapshot gather(@Nonnull PlayerRef player, @Nullable GameSession session) {
        if (session == null || !EZController.isEz(session))
            return null;
        return snapshot(GameStore.withStore(player, session.getId()).orElse(null));
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        cmd.set("#WinnerBanner.Background", "GG/WinnerBanner.png");
    }

    @Override
    public boolean render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session,
            @Nonnull HudDataSnapshot data) {
        if (!(data instanceof WinnerHudSnapshot winnerHud)) {
            GaiaLog.atWarning().withSession(session)
                    .log("Attempted to update the winner HUD with the incorrect snapshot!");
            return false;
        }
        if (!GameStore.isIn(player, winnerHud.game))
            return false;
        var match = winnerHud.standings;
        var teams = winnerHud.teams;

        var winner = match.isEmpty() ? null : TeamUtils.teamOf(teams, match.getFirst().getTeamId()).orElse(null);
        cmd.set("#WinnerName.Text",
                winner == null ? "NO WINNER" : TeamUtils.displayName(winner).toUpperCase(Locale.ROOT));
        var icon = winner == null ? "" : winner.getUiIcon();
        cmd.set("#WinnerIconLeft.Visible", !icon.isBlank());
        cmd.set("#WinnerIconRight.Visible", !icon.isBlank());
        if (!icon.isBlank()) {
            cmd.set("#WinnerIconLeft.Background", icon);
            cmd.set("#WinnerIconRight.Background", icon);
        }

        for (int i = 0; i < PODIUM.length; i++) {
            var column = PODIUM[i];
            var standing = i < match.size() ? match.get(i) : null;
            var team = standing == null ? null : teamOf(teams, standing);
            cmd.set(column + ".Visible", standing != null);
            if (standing == null)
                continue;

            cmd.set(column + " #Name.Text", team == null ? standing.getTeamId() : TeamUtils.displayName(team));
            cmd.set(column + " #Points.Text", "+" + standing.getPoints() + " pts");
            cmd.set(column + " #EventTotal.Text", standing.getEventTotal() + " event total");
            cmd.set(column + " #Initial.Visible", true);
            if (team != null) {
                TeamUtils.icon(cmd, column + " #Icon", column + " #Initial", team);
            } else {
                cmd.set(column + " #Initial.Text", "?");
            }
        }
        return true;
    }

    @Nullable
    private static TeamComponent teamOf(@Nullable TeamListComponent teams, @Nonnull Standing standing) {
        return teams == null ? null : teams.get(standing.getTeamId());
    }
}
