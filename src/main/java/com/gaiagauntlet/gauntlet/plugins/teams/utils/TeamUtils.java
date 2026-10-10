package com.gaiagauntlet.gauntlet.plugins.teams.utils;

import com.gaiagauntlet.gauntlet.core.party.components.PartyComponent;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.auto.components.Standing;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamListComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamListAsset;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.TeamPlayerComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.ui.PartyHud;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TeamUtils {
    // Static utils for getting stuff about a team / list of teams

    public static boolean hasOnline(TeamComponent team) {
        for (UUID uuid : team.getPlayers()) {
            var playerRef = Universe.get().getPlayer(uuid);
            if (Objects.nonNull(playerRef))
                return true;
        }
        return false;
    }

    /** Creates a new team component if it is missing */
    @Nonnull
    public static TeamListComponent withTeamList(World world, String session) {
        return GameStore.ensureStore(world, session).ensure(TeamListComponent.getGameComponentType(),
                TeamListComponent::new);
    }

    /** Creates a new team component if it is missing */
    @Nonnull
    public static TeamListComponent withTeamList(ComponentAccessor<EntityStore> accessor, String sessionId) {
        return GameStore.ensureStore(accessor, sessionId).ensure(TeamListComponent.getGameComponentType(),
                TeamListComponent::new);
    }

    /** Creates a new team list if it is missing */
    @Nullable
    public static TeamComponent withTeam(World world, String sessionId, String teamId) {
        return withTeamList(world, sessionId).get(teamId);
    }

    /** Creates a new team list if it is missing */
    @Nullable
    public static TeamComponent withTeam(ComponentAccessor<EntityStore> accessor, String sessionId, String teamId) {
        return withTeamList(accessor, sessionId).get(teamId);
    }

    @Nullable
    public static TeamComponent withTeamFor(World world, String session, UUID player) {
        var teams = withTeamList(world, session);
        return teams.get(player);
    }

    /**
     * Checks if a team has any alive players. Truthy if and only if a player on
     * this team is online and is not eliminated.
     */
    public static boolean hasAlive(TeamComponent team) {
        for (UUID uuid : team.getPlayers()) {
            var playerRef = Universe.get().getPlayer(uuid);
            if (Objects.isNull(playerRef))
                continue;
            var ref = playerRef.getReference();
            assert ref != null;
            var eliminated = ref.getStore().getComponent(ref, EliminatedComponent.getComponentType());
            if (Objects.isNull(eliminated))
                return true;
        }
        return false;
    }

    public static Collection<PlayerRef> getOnlinePlayers(TeamListComponent teams) {
        var onlinePlayers = new HashSet<PlayerRef>();
        for (UUID player : teams.getPlayers()) {
            PlayerRef playerRef = Universe.get().getPlayer(player);
            if (Objects.nonNull(playerRef))
                onlinePlayers.add(playerRef);
        }
        return onlinePlayers;
    }

    public static Collection<PlayerRef> getOnlinePlayers(TeamComponent team) {
        var onlinePlayers = new HashSet<PlayerRef>();
        for (UUID player : team.getPlayers()) {
            PlayerRef playerRef = Universe.get().getPlayer(player);
            if (Objects.nonNull(playerRef))
                onlinePlayers.add(playerRef);
        }
        return onlinePlayers;
    }

    public static TeamListComponent from(TeamListAsset asset) {
        var teamSession = new TeamListComponent();

        for (var team : asset.getTeamList().entrySet()) {
            teamSession.addTeam(team.getKey(), team.getValue().clone());
        }

        return teamSession;
    }

    public static double getScore(PlayerRef player) {
        if (player == null)
            return 0;
        var playerComponent = player.getComponentConcurrent(TeamPlayerComponent.getComponentType());
        if (playerComponent == null)
            return 0;
        return playerComponent.getKills();
    }

    public static double getScore(UUID playerId) {
        return getScore(PlayerUtils.get(playerId));
    }

    public static double getScore(TeamListComponent team, String teamId) {
        if (team == null || teamId == null)
            return 0d;
        return getScore(team.get(teamId));
    }

    public static double getScore(TeamComponent team) {
        if (team == null)
            return 0d;
        return team.getPlayers().stream().mapToDouble(TeamUtils::getScore).sum();
    }

    public static double getScore(TeamListComponent teams) {
        if (teams == null)
            return 0d;
        return teams.getTeams().values().stream()
                .mapToDouble(TeamUtils::getScore).sum();
    }

    /**
     * Initialise teams to a team list component from an asset. Does not populate
     * teams beyond what
     * has been manually defined in the {@code TeamListAsset}
     */
    public static void initialiseTeams(TeamListComponent teams, TeamListAsset teamAsset) {
        for (TeamComponent team : teamAsset.getTeamList().values()) {
            teams.addTeam(team.getId(), team.clone());
        }
    }

    /**
     * Distribute players across existing team components. Any players already in a
     * team remain
     * there, and new players are fit in appropriately
     */
    public static void distributePlayers(Collection<PlayerRef> players, TeamListComponent teams) {
        var unassigned = new LinkedHashSet<>(players);
        // First pass - Clean offline players, fill from parties if enabled.
        for (var team : teams.getTeams().values()) {
            for (UUID uuid : team.getPlayers()) {
                // Remove offline/uncalled-for players from team
                PlayerRef player = PlayerUtils.get(uuid);
                if (Objects.isNull(player) || !players.contains(player)) {
                    team.remove(uuid);
                    continue;
                }
                // Remove players already assigned to team from unassigned
                unassigned.remove(player);
            }

            // Check if any parties will fit in this team.
            for (PartyComponent party : PartyUtils.getParties()) {
                if (team.getSize() + party.getAllOnlinePlayers().size() <= teams.getTeamSize()) {
                    // Party fits, add all players to the team
                    for (PlayerRef player : party.getAllOnlinePlayers()) {
                        // Don't include players not requested to be distributed
                        if (!unassigned.contains(player))
                            continue;
                        party.addPlayer(player.getUuid());
                        unassigned.remove(player);
                    }
                }
            }
        }
    }

    public static final String DEAD_ICON = "GG/Dead.png";
    public static final String DISCONNECTED_ICON = "GG/Disconnected.png";

    @Nullable
    public static Optional<TeamListComponent> teamsOf(@Nullable GameSession session) {
        return session == null ? Optional.empty() : session.get(TeamListComponent.getSessionComponentType());
    }

    @Nullable
    public static Optional<TeamComponent> teamOf(@Nullable GameSession session, @Nonnull UUID player) {
        var teams = teamsOf(session);
        return teams.map(team -> team.get(player));
    }
    @Nullable
    public static Optional<TeamComponent> teamOf(@Nullable TeamListComponent teamList, @Nonnull String teamId) {
        var team = teamList.get(teamId);
        return Optional.ofNullable(team);
    }

    @Nonnull
    public static List<TeamComponent> sortedTeams(@Nonnull TeamListComponent teamList) {
        var teams = new ArrayList<>(teamList.getTeams().values());
        teams.sort((a, b) -> a.getId().compareToIgnoreCase(b.getId()));
        return teams;
    }

    /** The team's players, ordered by name. */
    @Nonnull
    public static List<UUID> membersOf(@Nonnull TeamComponent team) {
        var members = new ArrayList<>(team.getPlayers());
        members.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(PlayerUtils.nameFor(a), PlayerUtils.nameFor(b)));
        return members;
    }

    @Nonnull
    public static String displayName(@Nonnull TeamComponent team) {
        return team.getName().isEmpty() ? team.getId() : team.getName();
    }

    public static boolean isEliminated(@Nonnull UUID player) {
        var ref = PlayerUtils.get(player);
        return ref != null && ref.getComponentConcurrent(EliminatedComponent.getComponentType()) != null;
    }

    public static int score(@Nonnull UUID player) {
        return (int) Math.round(TeamUtils.getScore(player));
    }

    public static int score(@Nonnull TeamComponent team) {
        return (int) Math.round(TeamUtils.getScore(team));
    }

    /**
     * Shows the team's icon, or its initial on the tile the markup draws when it
     * has none.
     */
    public static void icon(@Nonnull UICommandBuilder cmd, @Nonnull String icon, @Nonnull String initial,
            @Nonnull TeamComponent team) {
        var path = team.getUiIcon();
        if (!path.isBlank()) {
            cmd.set(icon + ".Background", path);
            cmd.set(initial + ".Visible", false);
            return;
        }
        var name = displayName(team);
        cmd.set(initial + ".Text", name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT));
    }

    /**
     * Where a panel stacked just below the viewer's party panel starts. That panel
     * is shorter for a team that only watches.
     */
    public static int belowParty(@Nullable GameSession session, @Nonnull UUID player) {
        var own = teamOf(session, player).orElse(null);
        if (own == null) {
            return PartyHud.TOP;
        }
        return PartyHud.TOP + (own.isParticipant() ? PartyHud.HEIGHT : PartyHud.SPECTATING_HEIGHT) + 10;
    }

    /**
     * Participating teams with players, most kills first, each with its kills as
     * points.
     */
    @Nonnull
    public static List<Standing> standingsByKills(@Nonnull GameSession session) {
        var teams = teamsOf(session).orElse(null);
        if (teams == null)
            return List.of();
        return teams.getTeams().values().stream()
                .filter(t -> t.isParticipant() && t.getSize() > 0)
                .sorted(Comparator.comparingDouble((TeamComponent t) -> TeamUtils.getScore(t)).reversed()
                        .thenComparing(TeamComponent::getId))
                .map(t -> new Standing(t.getId(), (int) Math.round(TeamUtils.getScore(t)),
                        (int) Math.round(t.getScore())))
                .toList();
    }
}
