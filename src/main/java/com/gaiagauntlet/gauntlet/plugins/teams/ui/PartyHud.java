package com.gaiagauntlet.gauntlet.plugins.teams.ui;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.huds.HudWidgets;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.plugins.teams.TeamsPlugin;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The viewer's own team: icon, name and team score over a card per member with dead and disconnected
 * overlays. The cards are rebuilt whenever the viewer's team or its members change.
 */
public final class PartyHud implements HudElement {

    static final int TOP = 52;
    static final int HEIGHT = 178;
    static final int SPECTATING_HEIGHT = 64;
    private static final int RIGHT = 12;
    private static final int WIDTH = 300;
    private static final String MEMBER = "Gauntlet/Plugins/" + TeamsPlugin.ID + "/PartyMember.ui";

    private final HudWidgets.Sent sent = new HudWidgets.Sent();

    /** The team and members the cards were built for, or null when they need building. */
    @Nullable private String builtTeam;
    private List<UUID> builtMembers = List.of();

    @Nonnull @Override public String getId() {
        return "Party";
    }

    @Nonnull @Override public String getMarkup() {
        return "Gauntlet/Plugins/" + TeamsPlugin.ID + "/PartyHud.ui";
    }

    @Override public int getOrder() {
        return 30;
    }

    @Override
    public boolean isVisible(@Nonnull PlayerRef player, @Nullable GameSession session) {
        return TeamUi.teamOf(session, player.getUuid()) != null;
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        builtTeam = null;
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        var team = TeamUi.teamOf(session, player.getUuid());
        if (team == null) {
            return;
        }
        var members = team.isParticipant() ? TeamUi.membersOf(team) : List.<UUID>of();
        if (!team.getId().equals(builtTeam) || !members.equals(builtMembers)) {
            build(cmd, team, members);
        }
        if (!team.isParticipant()) {
            return;
        }
        sent.text(cmd, "#PartyScore", Integer.toString(TeamUi.score(team)));
        for (int i = 0; i < members.size(); i++) {
            var member = members.get(i);
            var card = "#PartyMembers[" + i + "]";
            var online = PlayerUtils.isOnline(member);
            sent.visible(cmd, card + " #Dead", online && TeamUi.isEliminated(member));
            sent.visible(cmd, card + " #Off", !online);
            sent.text(cmd, card + " #Score", TeamUi.score(member) + " pts");
        }
    }

    private void build(@Nonnull UICommandBuilder cmd, @Nonnull TeamComponent team, @Nonnull List<UUID> members) {
        sent.clear();
        builtTeam = team.getId();
        builtMembers = List.copyOf(members);

        var participant = team.isParticipant();
        HudWidgets.anchor(cmd, "#Party", TOP, RIGHT, WIDTH, participant ? HEIGHT : SPECTATING_HEIGHT);
        cmd.set("#PartyName.Text", TeamUi.displayName(team));
        TeamUi.icon(cmd, "#PartyIcon", "#PartyInitial", team);
        cmd.set("#PartySpectating.Visible", !participant);
        cmd.set("#PartyStat.Visible", participant);
        cmd.set("#PartyDivider.Visible", participant);
        cmd.set("#PartyMembers.Visible", participant);

        cmd.clear("#PartyMembers");
        for (int i = 0; i < members.size(); i++) {
            var card = "#PartyMembers[" + i + "]";
            cmd.append("#PartyMembers", MEMBER);
            cmd.set(card + " #Portrait.PlayerUuid", members.get(i).toString());
            cmd.set(card + " #Name.Text", TeamUi.nameOf(members.get(i)));
            cmd.set(card + " #DeadIcon.Background", TeamUi.DEAD_ICON);
            cmd.set(card + " #OffIcon.Background", TeamUi.DISCONNECTED_ICON);
        }
    }
}
