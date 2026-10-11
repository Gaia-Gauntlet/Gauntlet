package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.hud.huds.HudWidgets;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudDataSnapshot;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.plugins.teams.ui.PartyHud;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Event standings in the lobby, in the party panel's spot: every participating team ranked by its event
 * score, the viewer's own team highlighted. Takes the teams sidebar's place until the match reaches
 * the arena.
 */
public final class ScoreboardHud implements HudElement {

    private static final String ROW = "Gauntlet/Games/EliminationZone/ScoreRow.ui";
    private static final int RIGHT = 12;
    private static final int WIDTH = 250;
    private static final int TITLE_HEIGHT = 22;
    private static final int ROW_HEIGHT = 34;
    private static final int PADDING = 8;

    private final HudWidgets.Sent sent = new HudWidgets.Sent();

    /** The team ids in row order the rows were built for, or null when they need building. */
    @Nullable private List<String> builtTeams;

    @Nonnull @Override public String getId() {
        return "Scoreboard";
    }

    @Nonnull @Override public String getMarkup() {
        return "Gauntlet/Games/EliminationZone/ScoreboardHud.ui";
    }

    @Override public int getOrder() {
        return 40;
    }

    private record ScoreboardSnapshot(
        List<TeamComponent> teams,
        TeamComponent own
    ) implements HudDataSnapshot {};

    @Override
    public HudDataSnapshot gather(@Nonnull PlayerRef player, @Nullable GameSession session) {
         // return EZController.isEz(session) && MatchUtils.get(session) != null && MatchUtils.inLobby(session)
        //         && !ranked(session).isEmpty();
        if (session == null || !session.available() || !EZController.isEz(session)) return null; // hide if in an active game session

        var teams = ranked(session);
        var own = TeamUtils.teamOf(session, player.getUuid()).orElse(null);
        return new ScoreboardSnapshot(teams, own);
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        builtTeams = null;
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session, HudDataSnapshot data) {
        if (!(data instanceof ScoreboardSnapshot scoreboard)) {
            GaiaLog.atWarning().withSession(session).log("Attempted to update the scoreboard with the incorrect snapshot!");
            return;
        }

        var teams = scoreboard.teams;
        var own = scoreboard.own;
        var ids = teams.stream().map(TeamComponent::getId).toList();
        if (!ids.equals(builtTeams)) {
            build(cmd, teams);
        }

        for (int i = 0; i < teams.size(); i++) {
            var team = teams.get(i);
            var row = "#ScoreRows[" + i + "]";
            sent.text(cmd, row + " #Place", Integer.toString(i + 1));
            sent.text(cmd, row + " #Points", Long.toString(Math.round(team.getScore())));
            sent.visible(cmd, row + " #Highlight", team == own);
        }
    }

    private void build(@Nonnull UICommandBuilder cmd, @Nonnull List<TeamComponent> teams) {
        sent.clear();
        builtTeams = teams.stream().map(TeamComponent::getId).toList();

        HudWidgets.anchor(cmd, "#Scoreboard", PartyHud.TOP, RIGHT, WIDTH, PADDING * 2 + TITLE_HEIGHT + ROW_HEIGHT * teams.size());
        cmd.clear("#ScoreRows");
        for (int i = 0; i < teams.size(); i++) {
            var row = "#ScoreRows[" + i + "]";
            cmd.append("#ScoreRows", ROW);
            cmd.set(row + " #Name.Text", TeamUtils.displayName(teams.get(i)));
            TeamUtils.icon(cmd, row + " #Icon", row + " #Initial", teams.get(i));
        }
    }

    /** Participating teams with players, highest event score first, then by name. */
    @Nonnull
    private static List<TeamComponent> ranked(@Nullable GameSession session) {
        var teamList = TeamUtils.teamsOf(session).orElse(null);
        if (teamList == null) return List.of();
        return teamList.getTeams().values().stream()
                .filter(team -> team.isParticipant() && team.getSize() > 0)
                .sorted(Comparator.comparingDouble(TeamComponent::getScore).reversed()
                        .thenComparing(TeamUtils::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
