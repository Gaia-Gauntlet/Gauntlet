package com.gaiagauntlet.gauntlet.plugins.teams.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.huds.HudWidgets;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.plugins.gamestate.utils.MatchUtils;
import com.gaiagauntlet.gauntlet.plugins.teams.TeamsPlugin;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Every other participating team in the viewer's session, below the party panel: one row per team
 * with its members' portraits and scores. A row hides once nobody on it is online and standing.
 * Games that track their match hand this spot to their lobby scoreboard until the match reaches the arena.
 */
public final class TeamsHud implements HudElement {

    private static final int RIGHT = 12;
    private static final int WIDTH = 250;
    private static final String ROW = "Gauntlet/Plugins/" + TeamsPlugin.ID + "/TeamsHudRow.ui";
    private static final String MEMBER = "Gauntlet/Plugins/" + TeamsPlugin.ID + "/TeamsHudMember.ui";

    private final HudWidgets.Sent sent = new HudWidgets.Sent();

    /** Each row's team and members as they were built, or null when the rows need building. */
    @Nullable private List<List<UUID>> builtRows;
    private List<String> builtTeams = List.of();
    private int builtTop = -1;

    @Nonnull @Override public String getId() {
        return "Teams";
    }

    @Nonnull @Override public String getMarkup() {
        return "Gauntlet/Plugins/" + TeamsPlugin.ID + "/TeamsHud.ui";
    }

    @Override public int getOrder() {
        return 40;
    }

    @Override
    public boolean isVisible(@Nonnull PlayerRef player, @Nullable GameSession session) {
        if (MatchUtils.get(session) != null && MatchUtils.inLobby(session)) {
            return false;
        }
        return !otherTeams(player, session).isEmpty();
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        builtRows = null;
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        var teams = otherTeams(player, session);
        var rows = new ArrayList<List<UUID>>();
        for (var team : teams) {
            rows.add(TeamUi.membersOf(team));
        }
        var ids = teams.stream().map(TeamComponent::getId).toList();
        var top = TeamUi.belowParty(session, player.getUuid());
        if (!rows.equals(builtRows) || !ids.equals(builtTeams) || top != builtTop) {
            build(cmd, teams, rows, top);
        }

        for (int t = 0; t < teams.size(); t++) {
            var row = "#TeamsSidebar[" + t + "]";
            var members = rows.get(t);
            var standing = false;
            for (int m = 0; m < members.size(); m++) {
                var member = members.get(m);
                var card = row + " #Members[" + m + "]";
                var online = PlayerUtils.isOnline(member);
                var alive = !TeamUi.isEliminated(member);
                standing |= online && alive;
                sent.visible(cmd, card + " #Dead", online && !alive);
                sent.visible(cmd, card + " #Off", !online);
                sent.text(cmd, card + " #Score", Integer.toString(TeamUi.score(member)));
            }
            sent.visible(cmd, row, standing);
            sent.text(cmd, row + " #TeamScore", Integer.toString(TeamUi.score(teams.get(t))));
        }
    }

    private void build(@Nonnull UICommandBuilder cmd, @Nonnull List<TeamComponent> teams, @Nonnull List<List<UUID>> rows, int top) {
        sent.clear();
        builtRows = List.copyOf(rows);
        builtTeams = teams.stream().map(TeamComponent::getId).toList();
        builtTop = top;

        HudWidgets.anchor(cmd, "#TeamsSidebar", top, RIGHT, WIDTH, null);
        cmd.clear("#TeamsSidebar");
        for (int t = 0; t < teams.size(); t++) {
            var row = "#TeamsSidebar[" + t + "]";
            cmd.append("#TeamsSidebar", ROW);
            TeamUi.icon(cmd, row + " #Icon", row + " #Initial", teams.get(t));
            var members = rows.get(t);
            for (int m = 0; m < members.size(); m++) {
                var card = row + " #Members[" + m + "]";
                cmd.append(row + " #Members", MEMBER);
                cmd.set(card + " #Portrait.PlayerUuid", members.get(m).toString());
                cmd.set(card + " #DeadIcon.Background", TeamUi.DEAD_ICON);
                cmd.set(card + " #OffIcon.Background", TeamUi.DISCONNECTED_ICON);
            }
        }
    }

    /** Participating teams other than the viewer's that have members, ordered by id. */
    @Nonnull
    private static List<TeamComponent> otherTeams(@Nonnull PlayerRef player, @Nullable GameSession session) {
        var teamList = TeamUi.teamsOf(session);
        if (teamList == null) {
            return List.of();
        }
        var own = teamList.get(player.getUuid());
        var teams = new ArrayList<TeamComponent>();
        for (var team : TeamUi.sortedTeams(teamList)) {
            if (team.isParticipant() && team != own && team.getSize() > 0) {
                teams.add(team);
            }
        }
        return teams;
    }
}
