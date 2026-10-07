package com.gaiagauntlet.gauntlet.plugins.gamestate.utils;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.gamestate.components.MatchComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestate.components.Standing;
import com.gaiagauntlet.gauntlet.plugins.gamestate.constants.MatchState;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;

/**
 * Reads and moves a session's match. Every change is applied on the hub thread, so callers on any
 * thread can use these. Reads are safe anywhere.
 */
public final class MatchUtils {

    public static final int PRE_PORTAL_SECONDS = 300;
    public static final int POST_PORTAL_SECONDS = 120;
    public static final int END_SCREEN_SECONDS = 30;

    private MatchUtils() {
    }

    @Nullable
    public static MatchComponent get(@Nullable GameSession session) {
        var type = MatchComponent.getSessionComponentType();
        return session == null || type == null ? null : session.get(type).orElse(null);
    }

    @Nonnull
    public static MatchState phase(@Nullable GameSession session) {
        var match = get(session);
        return match == null ? MatchState.IDLE : match.getState();
    }

    /** Whether the session's players are in the hub rather than the arena. */
    public static boolean inLobby(@Nullable GameSession session) {
        return switch (phase(session)) {
            case IDLE, LOBBY_COUNTDOWN, PORTAL_OPEN, TRANSFERRING -> true;
            default -> false;
        };
    }

    /** Whether the match is being played out in the arena. */
    public static boolean inArena(@Nullable GameSession session) {
        return switch (phase(session)) {
            case STAGING, ACTIVE, SUDDEN_DEATH -> true;
            default -> false;
        };
    }

    /** Formats seconds as MM:SS. */
    @Nonnull
    public static String clock(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    /** Moves the match to a phase, with a countdown of the given seconds, or none when 0. */
    public static void transition(@Nonnull GameSession session, @Nonnull MatchState to, int countdownSeconds) {
        onHub(() -> transitionNow(session, to, countdownSeconds));
    }

    /**
     * Ends the match with the given standings, best first, and starts the end screen. When there is
     * more than one game to pick from, the session votes on its next game during the end screen.
     */
    public static void end(@Nonnull GameSession session, @Nonnull List<Standing> standings) {
        onHub(() -> {
            ensure(session).setStandings(standings);
            transitionNow(session, MatchState.ENDED, END_SCREEN_SECONDS);
            var games = VoteUtils.allGames();
            if (games.size() > 1) {
                VoteUtils.open(session, games, END_SCREEN_SECONDS);
            }
        });
    }

    /** Runs the current countdown out now, moving on as if it had expired. */
    public static void skip(@Nonnull GameSession session) {
        onHub(() -> advance(session));
    }

    public static void pause(@Nonnull GameSession session) {
        onHub(() -> ensure(session).pause(System.currentTimeMillis()));
    }

    public static void resume(@Nonnull GameSession session) {
        onHub(() -> ensure(session).resume(System.currentTimeMillis()));
    }

    /** Restarts the current phase's countdown with the given seconds. */
    public static void setTimer(@Nonnull GameSession session, int seconds) {
        onHub(() -> ensure(session).startCountdown(seconds, System.currentTimeMillis()));
    }

    /** Advances every session whose countdown has run out. Runs on the hub thread. */
    public static void tick() {
        var now = System.currentTimeMillis();
        for (var session : GauntletUtils.withResource().getSessions().values()) {
            var match = get(session);
            if (match != null && match.isExpired(now)) {
                advance(session);
            }
        }
    }

    /** The phase a countdown leads into when it runs out, and that phase's own countdown. */
    private static void advance(@Nonnull GameSession session) {
        var match = ensure(session);
        switch (match.getState()) {
            case LOBBY_COUNTDOWN -> transitionNow(session, MatchState.PORTAL_OPEN, POST_PORTAL_SECONDS);
            case PORTAL_OPEN -> transitionNow(session, MatchState.TRANSFERRING, 0);
            case STAGING -> transitionNow(session, MatchState.ACTIVE, 0);
            case ENDED -> transitionNow(session, MatchState.RETURNING, 0);
            default -> match.clearCountdown();
        }
    }

    private static void transitionNow(@Nonnull GameSession session, @Nonnull MatchState to, int countdownSeconds) {
        var match = ensure(session);
        var from = match.getState();
        if (to == MatchState.LOBBY_COUNTDOWN || to == MatchState.IDLE) {
            match.setStandings(List.of());
        }
        match.setState(to);
        if (countdownSeconds > 0) {
            match.startCountdown(countdownSeconds, System.currentTimeMillis());
        } else {
            match.clearCountdown();
        }
        GauntletEventRegistry.dispatch(new MatchStateEvent(session.getId(), from, to));
    }

    @Nonnull
    private static MatchComponent ensure(@Nonnull GameSession session) {
        return session.ensure(MatchComponent.getSessionComponentType(), new MatchComponent());
    }

    private static void onHub(@Nonnull Runnable task) {
        GauntletUtils.run(GauntletUtils.withHubWorld(), task);
    }
}
