package com.gaiagauntlet.gauntlet.plugins.teams.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.hud.huds.HudWidgets;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudDataSnapshot;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.teams.TeamsPlugin;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamListComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent.EliminationReason;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Every other participating team in the viewer's session, below the party
 * panel: one row per team
 * with its members' portraits and scores. A row hides once nobody on it is
 * online and standing.
 * Games that track their match hand this spot to their lobby scoreboard until
 * the match reaches the arena.
 */
public final class TeamsHud implements HudElement {

    public static final String ID = "Teams";

    private static final int RIGHT = 12;
    private static final int WIDTH = 250;
    private static final String ROW = "Gauntlet/Plugins/" + TeamsPlugin.ID + "/TeamsHudRow.ui";
    private static final String MEMBER = "Gauntlet/Plugins/" + TeamsPlugin.ID + "/TeamsHudMember.ui";

    private final HudWidgets.Sent sent = new HudWidgets.Sent();

    /**
     * Each row's team and members as they were built, or null when the rows need
     * building.
     */
    @Nullable
    private List<List<UUID>> builtRows;
    private List<String> builtTeams = List.of();
    private int builtTop = -1;

    /**
     * A game's teams as the party and teams panels draw them. Players disconnecting
     * in this update are still in the universe, so they are listed to draw as
     * offline.
     */
    public record TeamListSnapshot(
            GameEcs game,
            TeamListComponent teams,
            Set<UUID> disconnected) implements HudDataSnapshot {

        public boolean isOnline(@Nonnull UUID player) { // should be in it's own util
            var playerRef = PlayerUtils.get(player);
            if (playerRef == null || !playerRef.isValid())
                return false;
            var eliminated = playerRef.getComponentConcurrent(EliminatedComponent.getComponentType());
            if (eliminated != null && eliminated.getReason() == EliminationReason.DISCONNECT)
                return false;
            return !disconnected.contains(player);
        }
    }

    @Nullable
    public static TeamListSnapshot snapshot(@Nullable GameEcs game, @Nonnull Set<UUID> disconnected) {
        var teams = TeamUtils.teamsOf(game).orElse(null);
        if (teams == null)
            return null;
        return new TeamListSnapshot(game, teams, Set.copyOf(disconnected));
    }

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    @Nonnull
    @Override
    public String getMarkup() {
        return "Gauntlet/Plugins/" + TeamsPlugin.ID + "/TeamsHud.ui";
    }

    @Override
    public int getOrder() {
        return 40;
    }

    @Override
    public TeamListSnapshot gather(@Nonnull PlayerRef player, @Nullable GameSession session) {
        if (session == null)
            return null;
        var snapshot = snapshot(GameStore.withStore(player, session.getId()).orElse(null), Set.of());
        if (snapshot == null || otherTeams(player, snapshot.teams).isEmpty())
            return null;
        return snapshot;
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        builtRows = null;
    }

    @Override
    public boolean render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session,
            @Nonnull HudDataSnapshot data) {
        if (!(data instanceof TeamListSnapshot teamData)) {
            GaiaLog.atWarning().withSession(session)
                    .log("Attempted to update the Teams Hud with the incorrect snapshot!");
            return false;
        }
        if (!GameStore.isIn(player, teamData.game))
            return false;
        var teams = otherTeams(player, teamData.teams);
        if (teams.isEmpty())
            return false;
        var rows = new ArrayList<List<UUID>>();
        for (var team : teams) {
            rows.add(TeamUtils.membersOf(team));
        }
        var ids = teams.stream().map(TeamComponent::getId).toList();
        var top = TeamUtils.belowParty(teamData.game, player.getUuid());
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
                var online = teamData.isOnline(member);
                var alive = !TeamUtils.isEliminated(member);
                standing |= online && alive;
                sent.visible(cmd, card + " #Dead", online && !alive);
                sent.visible(cmd, card + " #Off", !online);
                sent.text(cmd, card + " #Score", Integer.toString(TeamUtils.score(member)));
            }
            sent.visible(cmd, row, standing);
            sent.text(cmd, row + " #TeamScore", Integer.toString(TeamUtils.score(teams.get(t))));
        }
        return true;
    }

    private void build(@Nonnull UICommandBuilder cmd, @Nonnull List<TeamComponent> teams,
            @Nonnull List<List<UUID>> rows, int top) {
        sent.clear();
        builtRows = List.copyOf(rows);
        builtTeams = teams.stream().map(TeamComponent::getId).toList();
        builtTop = top;

        HudWidgets.anchor(cmd, "#TeamsSidebar", top, RIGHT, WIDTH, null);
        cmd.clear("#TeamsSidebar");
        for (int t = 0; t < teams.size(); t++) {
            var row = "#TeamsSidebar[" + t + "]";
            cmd.append("#TeamsSidebar", ROW);
            TeamUtils.icon(cmd, row + " #Icon", row + " #Initial", teams.get(t));
            var members = rows.get(t);
            for (int m = 0; m < members.size(); m++) {
                var card = row + " #Members[" + m + "]";
                cmd.append(row + " #Members", MEMBER);
                cmd.set(card + " #Portrait.PlayerUuid", members.get(m).toString());
                cmd.set(card + " #DeadIcon.Background", TeamUtils.DEAD_ICON);
                cmd.set(card + " #OffIcon.Background", TeamUtils.DISCONNECTED_ICON);
            }
        }
    }

    /**
     * Participating teams other than the viewer's that have members, ordered by id.
     */
    @Nonnull
    private static List<TeamComponent> otherTeams(@Nonnull PlayerRef player, @Nonnull TeamListComponent teamList) {
        var own = teamList.get(player.getUuid());
        var teams = new ArrayList<TeamComponent>();
        for (var team : TeamUtils.sortedTeams(teamList)) {
            if (team.isParticipant() && team != own && team.getSize() > 0) {
                teams.add(team);
            }
        }
        return teams;
    }
}
