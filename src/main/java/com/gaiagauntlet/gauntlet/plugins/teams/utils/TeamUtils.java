package com.gaiagauntlet.gauntlet.plugins.teams.utils;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.config.GauntletConfig;
import com.gaiagauntlet.gauntlet.core.party.components.PartyComponent;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.ContextComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamListComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamListAsset;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.TeamPlayerComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.ui.PartyHud;
import com.gaiagauntlet.gauntlet.plugins.teams.ui.TeamsHud;
import com.gaiagauntlet.gauntlet.core.hud.events.HudEvent;
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
     * Participant teams with a member still competing: in the arena and not eliminated. The players in
     * {@code out} are treated as gone, for eliminations and departures not yet applied to the store.
     */
    public static Collection<TeamComponent> withAliveTeams(World arena, String session, Set<UUID> out) {
        Collection<TeamComponent> aliveTeams = new HashSet<>();
        var store = arena.getEntityStore().getStore();
        for (TeamComponent team : TeamUtils.withTeamList(arena, session).getTeams().values()) {
            if (!team.isParticipant()) continue;
            for (UUID uuid : team.getPlayers()) {
                if (out.contains(uuid)) continue;
                var player = PlayerUtils.get(uuid);
                var ref = player == null ? null : player.getReference();
                if (ref == null || !ref.isValid() || ref.getStore() != store) continue;
                if (store.getComponent(ref, EliminatedComponent.getComponentType()) != null) continue;
                aliveTeams.add(team);
                break;
            }
        }
        return aliveTeams;
    }

    /** Players on a participant team who stand in the game's world and are not eliminated, treating {@code out} as gone. */
    public static int countAlive(@Nonnull GameEcs game, @Nonnull Set<UUID> out) {
        var teams = teamsOf(game).orElse(null);
        if (teams == null)
            return 0;
        var arenaId = GameStore.getContext(game).getWorld().getWorldConfig().getUuid();
        var alive = 0;
        // every team
        for (var team : teams.getTeams().values()) {
            if (!team.isParticipant())
                continue;
            // every player (ignoring non-participants like camera crew)
            for (var uuid : team.getPlayers()) {
                if (out.contains(uuid))
                    continue;
                if (competing(PlayerUtils.get(uuid), arenaId))
                    alive++;
            }
        }
        return alive;
    }

    /** Whether any player on the team stands in the game's world and is not eliminated. */
    public static boolean hasAlive(@Nonnull GameEcs game, @Nonnull TeamComponent team) {
        var arenaId = GameStore.getContext(game).getWorld().getWorldConfig().getUuid();
        for (var uuid : team.getPlayers()) {
            if (competing(PlayerUtils.get(uuid), arenaId))
                return true;
        }
        return false;
    }

    private static boolean competing(@Nullable PlayerRef player, @Nonnull UUID arenaId) {
        return player != null && arenaId.equals(player.getWorldUuid())
                && player.getComponentConcurrent(EliminatedComponent.getComponentType()) == null;
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

        teamSession.setTeamSize(asset.getTeamSize());
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
     * Places every player who is not on a team yet. Party members still to be placed go together to the
     * fullest team with room for all of them, joining a teammate's team when one of them is already
     * placed. A party no team can hold is split across the fullest teams with room. Teams fill up
     * before an empty one is opened. Players stay unplaced only when every team is full.
     */
    public static void distributePlayers(@Nonnull Collection<PlayerRef> players, @Nonnull TeamListComponent teams) {
        var groups = new LinkedHashMap<String, List<UUID>>();
        for (var player : players) {
            var uuid = player.getUuid();
            if (teams.get(uuid) != null)
                continue;
            var key = PartyUtils.getParty(player).map(PartyComponent::getId).orElse(uuid.toString());
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(uuid);
        }
        var ordered = new ArrayList<>(groups.entrySet());
        ordered.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));
        for (var group : ordered) {
            var members = group.getValue();
            var target = partyTeam(group.getKey(), teams, members.size());
            if (target == null)
                target = fullestWithRoom(teams, members.size());
            for (var member : members) {
                var team = target != null ? target : fullestWithRoom(teams, 1);
                if (team == null) {
                    GaiaLog.atWarning().log("Every team is full, " + PlayerUtils.nameFor(member) + " was not placed");
                    continue;
                }
                teams.put(member, team.getId());
            }
        }
    }

    @Nullable
    private static TeamComponent partyTeam(@Nonnull String partyId, @Nonnull TeamListComponent teams, int needed) {
        var party = PartyUtils.getParty(partyId).orElse(null);
        if (party == null)
            return null;
        for (var member : party.getAllPlayers()) {
            var team = teams.get(member);
            if (team != null && team.isParticipant() && hasRoom(teams, team, needed))
                return team;
        }
        return null;
    }

    /** Without a team size there is nothing to fill, so players are spread across the smallest teams instead. */
    @Nullable
    private static TeamComponent fullestWithRoom(@Nonnull TeamListComponent teams, int needed) {
        boolean fill = teams.getTeamSize() > 0;
        TeamComponent best = null;
        for (var team : sortedTeams(teams)) {
            if (team.isParticipant() && hasRoom(teams, team, needed)
                    && (best == null || (fill ? team.getSize() > best.getSize() : team.getSize() < best.getSize())))
                best = team;
        }
        return best;
    }

    private static boolean hasRoom(@Nonnull TeamListComponent teams, @Nonnull TeamComponent team, int needed) {
        return teams.getTeamSize() <= 0 || team.getSize() + needed <= teams.getTeamSize();
    }

    @Nonnull
    public static TeamListComponent defaultTeams() {
        var presetId = GauntletConfig.get().getTeamsPreset();
        var preset = presetId == null ? null : TeamListAsset.getAssetMap().get(presetId);
        if (preset == null) {
            GaiaLog.atWarning().log("Team preset " + presetId + " does not exist, the game starts without teams");
            return new TeamListComponent();
        }
        return from(preset);
    }

    public static final String DEAD_ICON = "GG/Dead.png";
    public static final String DISCONNECTED_ICON = "GG/Disconnected.png";

    @Nullable
    public static Optional<TeamListComponent> teamsOf(@Nullable GameSession session) {
        return session == null ? Optional.empty() : session.get(TeamListComponent.getSessionComponentType());
    }

    @Nonnull
    public static Optional<TeamListComponent> teamsOf(@Nullable GameEcs game) {
        return game == null ? Optional.empty() : game.get(TeamListComponent.getGameComponentType());
    }

    @Nullable
    public static Optional<TeamComponent> teamOf(@Nullable GameSession session, @Nonnull UUID player) {
        var teams = teamsOf(session);
        return teams.map(team -> team.get(player));
    }

    @Nonnull
    public static Optional<TeamComponent> teamOf(@Nullable GameEcs game, @Nonnull UUID player) {
        return teamsOf(game).map(teams -> teams.get(player));
    }

    /** Redraws the party and teams panels for the game's session, drawing the given players as offline */
    public static void updateHuds(@Nonnull GameEcs game, @Nonnull Set<UUID> disconnected) {
        var snapshot = TeamsHud.snapshot(game, disconnected);
        var world = game.get(ContextComponent.getComponentType()).map(ContextComponent::getWorld).orElse(null);
        new HudEvent(game.getSessionId(), PartyHud.ID, snapshot, world).dispatch();
        new HudEvent(game.getSessionId(), TeamsHud.ID, snapshot, world).dispatch();
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
    public static int belowParty(@Nullable GameEcs game, @Nonnull UUID player) {
        var own = teamOf(game, player).orElse(null);
        if (own == null) {
            return PartyHud.TOP;
        }
        return PartyHud.TOP + (own.isParticipant() ? PartyHud.HEIGHT : PartyHud.SPECTATING_HEIGHT) + 10;
    }
}
