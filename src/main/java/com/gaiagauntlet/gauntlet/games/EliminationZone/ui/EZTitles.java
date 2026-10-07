package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestate.utils.MatchUtils;
import com.gaiagauntlet.gauntlet.plugins.teams.ui.TeamUi;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.EventTitleStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

/** The big titles an Elimination Zone match shows its players as it moves through its phases. */
public final class EZTitles {

    private static final String INFO = "#7EC8FF";
    private static final String SUCCESS = "#55FF55";
    private static final String DANGER = "#FF5555";
    private static final String SOUND_ALARM = "SFX_Discovery_Z4_Short";

    private EZTitles() {
    }

    public static void onMatchState(@Nonnull MatchStateEvent event) {
        var session = GauntletUtils.sessionFor(event.getSessionId()).orElse(null);
        if (!EZUi.isEz(session)) return;
        switch (event.getTo()) {
            case PORTAL_OPEN -> show(session, "THE PORTAL IS OPEN", "Get to the portal!", INFO, null);
            case ACTIVE -> show(session, "MATCH STARTED", "Last team standing wins", SUCCESS, null);
            case SUDDEN_DEATH -> show(session, "SUDDEN DEATH!", "Zones close twice as fast!", DANGER, SOUND_ALARM);
            case ENDED -> showWinner(session);
            default -> {
            }
        }
    }

    private static void showWinner(@Nonnull GameSession session) {
        var match = MatchUtils.get(session);
        var standings = match == null ? null : match.getStandings();
        var teams = TeamUi.teamsOf(session);
        var winner = standings == null || standings.isEmpty() || teams == null ? null : teams.get(standings.getFirst().getTeamId());
        if (winner == null) {
            show(session, "NO WINNER", "Everyone was eliminated", DANGER, null);
            return;
        }
        show(session, TeamUi.displayName(winner).toUpperCase(Locale.ROOT), "wins the match!", SUCCESS, null);
    }

    private static void show(@Nonnull GameSession session, @Nonnull String primary, @Nonnull String secondary,
            @Nonnull String color, @Nullable String sound) {
        var title = Message.raw(primary).color(color);
        var subtitle = Message.raw(secondary);
        for (var player : GauntletUtils.playersFor(session)) {
            EventTitleUtil.showEventTitleToPlayer(player, title, subtitle, EventTitleStyle.Major, null,
                    EventTitleUtil.DEFAULT_DURATION, EventTitleUtil.DEFAULT_FADE_DURATION, EventTitleUtil.DEFAULT_FADE_DURATION);
            if (sound != null) {
                SoundUtil.playSoundEvent2dToPlayer(player, sound, SoundCategory.UI);
            }
        }
    }
}
