package com.gaiagauntlet.gauntlet.plugins.teams.ui;

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
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.teams.TeamsPlugin;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.ui.TeamsHud.TeamListSnapshot;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The viewer's own team: icon, name and team score over a card per member with
 * dead and disconnected
 * overlays. The cards are rebuilt whenever the viewer's team or its members
 * change.
 * 
 * Note: This is NOT a core file, but rather a teams plugin file that renders party info
 */
public final class PartyHud implements HudElement {

    public static final String ID = "Party";
    public static final int TOP = 52;
    public static final int HEIGHT = 178;
    public static final int SPECTATING_HEIGHT = 64;
    private static final int RIGHT = 12;
    private static final int WIDTH = 300;
    private static final String MEMBER = "Gauntlet/Plugins/" + TeamsPlugin.ID + "/PartyMember.ui";

    private final HudWidgets.Sent sent = new HudWidgets.Sent();

    /**
     * The team and members the cards were built for, or null when they need
     * building.
     */
    @Nullable
    private String builtTeam;
    private List<UUID> builtMembers = List.of();

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    @Nonnull
    @Override
    public String getMarkup() {
        return "Gauntlet/Plugins/" + TeamsPlugin.ID + "/PartyHud.ui";
    }

    @Override
    public int getOrder() {
        return 30;
    }

    @Override
    public TeamListSnapshot gather(@Nonnull PlayerRef player, @Nullable GameSession session) {
        if (session == null)
            return null;
        var snapshot = TeamsHud.snapshot(GameStore.withStore(player, session.getId()).orElse(null), Set.of());
        if (snapshot == null || snapshot.teams().get(player.getUuid()) == null)
            return null;
        return snapshot;
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        builtTeam = null;
    }

    @Override
    public boolean render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session,
            @Nonnull HudDataSnapshot data) {
        if (!(data instanceof TeamListSnapshot partyHud)) {
            GaiaLog.atWarning().withSession(session)
                    .log("Attempted to update the Party Hud with the incorrect snapshot!");
            return false;
        }
        if (!GameStore.isIn(player, partyHud.game()))
            return false;
        var team = partyHud.teams().get(player.getUuid());
        if (team == null) {
            return false;
        }
        var members = team.isParticipant() ? TeamUtils.membersOf(team) : List.<UUID>of();
        if (!team.getId().equals(builtTeam) || !members.equals(builtMembers)) {
            build(cmd, team, members);
        }
        if (!team.isParticipant()) {
            return true;
        }
        sent.text(cmd, "#PartyScore", Integer.toString(TeamUtils.kills(team)));
        for (int i = 0; i < members.size(); i++) {
            var member = members.get(i);
            var card = "#PartyMembers[" + i + "]";
            var online = partyHud.isOnline(member);
            sent.visible(cmd, card + " #Dead", online && TeamUtils.isEliminated(member));
            sent.visible(cmd, card + " #Off", !online);
            sent.text(cmd, card + " #Score", TeamUtils.kills(member) + " kills");
        }
        return true;
    }

    private void build(@Nonnull UICommandBuilder cmd, @Nonnull TeamComponent team, @Nonnull List<UUID> members) {
        sent.clear();
        builtTeam = team.getId();
        builtMembers = List.copyOf(members);

        var participant = team.isParticipant();
        HudWidgets.anchor(cmd, "#Party", TOP, RIGHT, WIDTH, participant ? HEIGHT : SPECTATING_HEIGHT);
        cmd.set("#PartyName.Text", TeamUtils.displayName(team));
        TeamUtils.icon(cmd, "#PartyIcon", "#PartyInitial", team);
        cmd.set("#PartySpectating.Visible", !participant);
        cmd.set("#PartyStat.Visible", participant);
        cmd.set("#PartyDivider.Visible", participant);
        cmd.set("#PartyMembers.Visible", participant);

        cmd.clear("#PartyMembers");
        for (int i = 0; i < members.size(); i++) {
            var card = "#PartyMembers[" + i + "]";
            cmd.append("#PartyMembers", MEMBER);
            cmd.set(card + " #Portrait.PlayerUuid", members.get(i).toString());
            cmd.set(card + " #Name.Text", PlayerUtils.nameFor(members.get(i)));
            cmd.set(card + " #DeadIcon.Background", TeamUtils.DEAD_ICON);
            cmd.set(card + " #OffIcon.Background", TeamUtils.DISCONNECTED_ICON);
        }
    }
}
