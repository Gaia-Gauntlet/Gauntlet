package com.gaiagauntlet.gauntlet.plugins.gamestate.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.session.constants.SessionState;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.gaiagauntlet.gauntlet.core.ui.pages.Widgets;
import com.gaiagauntlet.gauntlet.plugins.gamestate.GameStatePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestate.components.MatchComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestate.components.Standing;
import com.gaiagauntlet.gauntlet.plugins.gamestate.constants.MatchState;
import com.gaiagauntlet.gauntlet.plugins.gamestate.utils.MatchUtils;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamListComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.Universe;

/** Where the match is, its countdown, the life cycle actions, and preflight. */
public final class MatchTab implements AdminTab {

    @Nonnull @Override public String getId() {
        return "Match";
    }

    @Nonnull @Override public String getPanel() {
        return "Gauntlet/Plugins/" + GameStatePlugin.ID + "/MatchPanel.ui";
    }

    @Override public int getOrder() {
        return 10;
    }

    @Override
    public void bind(@Nonnull UIEventBuilder evt) {
        Widgets.bind(evt, "#TimerSkip", "match.skip");
        Widgets.bind(evt, "#TimerPause", "match.pause");
        Widgets.bind(evt, "#TimerResume", "match.resume");
        Widgets.bindValues(evt, "#TimerSet", "match.timer", "", Map.of("@Num", "#TimerSeconds.Value"));
        Widgets.bind(evt, "#MatchStart", "match.start");
        Widgets.bind(evt, "#MatchPortal", "match.portal");
        Widgets.bind(evt, "#MatchTransfer", "match.transfer");
        Widgets.bind(evt, "#MatchSuddenDeath", "match.suddendeath");
        Widgets.bind(evt, "#MatchEnd", "match.end");
        Widgets.bind(evt, "#MatchStop", "match.stop");
        Widgets.bind(evt, "#ResetScores", "match.resetscores");
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        Widgets.field(cmd, "HubField", GauntletUtils.withHubWorld().getName());
        Widgets.field(cmd, "LobbiesField", "N/A");
        Widgets.field(cmd, "ArenaField", "N/A");
        Widgets.field(cmd, "PortalField", "N/A");
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        Widgets.field(cmd, "MatchGameField", Objects.isNull(session) ? "N/A" : session.getCurrentGame());
        Widgets.field(cmd, "MatchStateField", Objects.isNull(session) ? "N/A"
                : MatchUtils.phase(session).name() + " (session " + session.getSessionState().name() + ")");
        Widgets.field(cmd, "TimerField", timer(MatchUtils.get(session)));
        Widgets.field(cmd, "AliveField", alive(session));
        Widgets.fillList(cmd, "PreflightList", preflight(session), "Nothing to check");
    }

    @Nullable
    @Override
    public Message handle(@Nonnull String action, @Nonnull AdminPageEvent event, @Nullable GameSession session, @Nonnull AdminPage page) {
        if (session == null) {
            return Widgets.fail("Pick a session first");
        }
        var phase = MatchUtils.phase(session);
        return switch (action) {
            case "match.skip" -> {
                MatchUtils.skip(session);
                yield Widgets.ok("Skipped the " + phase.name() + " countdown");
            }
            case "match.pause" -> {
                MatchUtils.pause(session);
                yield Widgets.ok("Countdown paused");
            }
            case "match.resume" -> {
                MatchUtils.resume(session);
                yield Widgets.ok("Countdown resumed");
            }
            case "match.timer" -> {
                var seconds = Widgets.parseInt(event.num(), "seconds");
                if (seconds <= 0) yield Widgets.fail("The countdown needs at least one second");
                MatchUtils.setTimer(session, seconds);
                yield Widgets.ok("Countdown set to " + MatchUtils.clock(seconds));
            }
            case "match.start" -> {
                if (!MatchUtils.inLobby(session)) yield Widgets.fail("The match is already past the lobby (" + phase.name() + ")");
                MatchUtils.transition(session, MatchState.LOBBY_COUNTDOWN, MatchUtils.PRE_PORTAL_SECONDS);
                yield Widgets.ok("Lobby countdown started");
            }
            case "match.portal" -> {
                if (!MatchUtils.inLobby(session)) yield Widgets.fail("The match is already past the lobby (" + phase.name() + ")");
                MatchUtils.transition(session, MatchState.PORTAL_OPEN, MatchUtils.POST_PORTAL_SECONDS);
                yield Widgets.ok("Portal open");
            }
            case "match.transfer" -> {
                if (!MatchUtils.inLobby(session)) yield Widgets.fail("The match is already past the lobby (" + phase.name() + ")");
                MatchUtils.transition(session, MatchState.TRANSFERRING, 0);
                yield Widgets.ok("Transferring. The game moves players into the arena");
            }
            case "match.suddendeath" -> {
                if (phase != MatchState.ACTIVE) yield Widgets.fail("Sudden death needs a live match (" + phase.name() + ")");
                MatchUtils.transition(session, MatchState.SUDDEN_DEATH, 0);
                yield Widgets.ok("Sudden death");
            }
            case "match.end" -> {
                if (!MatchUtils.inArena(session)) yield Widgets.fail("There is no match in the arena to end (" + phase.name() + ")");
                MatchUtils.end(session, standingsByKills(session));
                yield Widgets.ok("Match ended, ranked by kills");
            }
            case "match.stop" -> {
                MatchUtils.transition(session, MatchState.RETURNING, 0);
                yield Widgets.ok("Match stopped. The game returns everyone to the lobby");
            }
            case "match.resetscores" -> {
                var teams = teamsOf(session);
                if (teams == null) yield Widgets.fail("This session has no teams");
                teams.getTeams().values().forEach(TeamComponent::clearScore);
                yield Widgets.ok("Event scores reset");
            }
            default -> null;
        };
    }

    @Nonnull
    private static String timer(@Nullable MatchComponent match) {
        if (match == null || !match.hasCountdown()) return "none";
        var clock = MatchUtils.clock(match.remainingSeconds(System.currentTimeMillis()));
        return match.isPaused() ? clock + " (paused)" : clock;
    }

    @Nonnull
    private static String alive(@Nullable GameSession session) {
        var teams = teamsOf(session);
        if (teams == null) return "no teams";
        int standing = 0;
        int total = 0;
        for (var team : teams.getTeams().values()) {
            if (!team.isParticipant()) continue;
            total += team.getSize();
            for (var player : team.getPlayers()) {
                var ref = Universe.get().getPlayer(player);
                if (ref != null && ref.getComponentConcurrent(EliminatedComponent.getComponentType()) == null) standing++;
            }
        }
        return standing + " of " + total + " players standing";
    }

    /** What would stop a match from running in this session. */
    @Nonnull
    private static List<String> preflight(@Nullable GameSession session) {
        var problems = new ArrayList<String>();
        if (session == null) return List.of("No session selected");
        if (session.getCurrentGame() == null || session.getCurrentGame().isEmpty()) problems.add("No game is set up, so set one up in the Session tab");
        if (session.getSessionState() != SessionState.RUNNING) problems.add("Session is " + session.getSessionState().name() + ", not RUNNING");
        var teams = teamsOf(session);
        if (teams == null) {
            problems.add("No teams in this session");
        } else if (teams.getTeams().values().stream().noneMatch(t -> t.isParticipant() && t.getSize() > 0)) {
            problems.add("No participating team has players");
        }
        if (GauntletUtils.playersFor(session).isEmpty()) problems.add("Nobody in this session is online");
        return problems.isEmpty() ? List.of("Ready") : problems;
    }

    /** Participating teams with players, most kills first, each with its kills as points. */
    @Nonnull
    private static List<Standing> standingsByKills(@Nonnull GameSession session) {
        var teams = teamsOf(session);
        if (teams == null) return List.of();
        return teams.getTeams().values().stream()
                .filter(t -> t.isParticipant() && t.getSize() > 0)
                .sorted(Comparator.comparingDouble((TeamComponent t) -> TeamUtils.getScore(t)).reversed().thenComparing(TeamComponent::getId))
                .map(t -> new Standing(t.getId(), (int) Math.round(TeamUtils.getScore(t)), (int) Math.round(t.getScore())))
                .toList();
    }

    @Nullable
    private static TeamListComponent teamsOf(@Nullable GameSession session) {
        var type = TeamListComponent.getSessionComponentType();
        return session == null || type == null ? null : session.get(type).orElse(null);
    }
}
