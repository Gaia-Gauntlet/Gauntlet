package com.gaiagauntlet.gauntlet.plugins.teams.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamListComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.Universe;

/** Lookups and drawing shared by the Teams plugin's admin tab and HUD elements, and by games drawing teams. */
public final class TeamUi {

    static final String DEAD_ICON = "GG/Dead.png";
    static final String DISCONNECTED_ICON = "GG/Disconnected.png";

    private TeamUi() {
    }

    @Nullable
    public static TeamListComponent teamsOf(@Nullable GameSession session) {
        var type = TeamListComponent.getSessionComponentType();
        return session == null || type == null ? null : session.get(type).orElse(null);
    }

    @Nullable
    public static TeamComponent teamOf(@Nullable GameSession session, @Nonnull UUID player) {
        var teams = teamsOf(session);
        return teams == null ? null : teams.get(player);
    }

    @Nonnull
    public static List<TeamComponent> sortedTeams(@Nonnull TeamListComponent teamList) {
        var teams = new ArrayList<>(teamList.getTeams().values());
        teams.sort((a, b) -> a.getId().compareToIgnoreCase(b.getId()));
        return teams;
    }

    /** The team's players, ordered by name. */
    @Nonnull
    static List<UUID> membersOf(@Nonnull TeamComponent team) {
        var members = new ArrayList<>(team.getPlayers());
        members.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(nameOf(a), nameOf(b)));
        return members;
    }

    @Nonnull
    public static String displayName(@Nonnull TeamComponent team) {
        return team.getName().isEmpty() ? team.getId() : team.getName();
    }

    @Nonnull
    static String nameOf(@Nonnull UUID player) {
        var online = PlayerUtils.resolveOnline(player);
        if (online != null) return online;
        var known = PlayerUtils.idsToPlayer.get(player);
        return known != null ? known : player.toString();
    }

    static boolean isEliminated(@Nonnull UUID player) {
        var ref = Universe.get().getPlayer(player);
        return ref != null && ref.getComponentConcurrent(EliminatedComponent.getComponentType()) != null;
    }

    static int score(@Nonnull UUID player) {
        return (int) Math.round(TeamUtils.getScore(player));
    }

    static int score(@Nonnull TeamComponent team) {
        return (int) Math.round(TeamUtils.getScore(team));
    }

    /** Shows the team's icon, or its initial on the tile the markup draws when it has none. */
    public static void icon(@Nonnull UICommandBuilder cmd, @Nonnull String icon, @Nonnull String initial, @Nonnull TeamComponent team) {
        var path = team.getUiIcon();
        if (!path.isBlank()) {
            cmd.set(icon + ".Background", path);
            cmd.set(initial + ".Visible", false);
            return;
        }
        var name = displayName(team);
        cmd.set(initial + ".Text", name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT));
    }

    /** Where a panel stacked just below the viewer's party panel starts. That panel is shorter for a team that only watches. */
    public static int belowParty(@Nullable GameSession session, @Nonnull UUID player) {
        var own = teamOf(session, player);
        if (own == null) {
            return PartyHud.TOP;
        }
        return PartyHud.TOP + (own.isParticipant() ? PartyHud.HEIGHT : PartyHud.SPECTATING_HEIGHT) + 10;
    }
}
