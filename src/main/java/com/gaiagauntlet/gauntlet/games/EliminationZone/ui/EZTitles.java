package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.EventTitleStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

/**
 * The big titles an Elimination Zone match shows its players as it moves through its phases. The end
 * screen has none, since the winner banner announces the result.
 */
public final class EZTitles {

    private static final String INFO = "#7EC8FF";
    private static final String SUCCESS = "#55FF55";
    private static final String DANGER = "#FF5555";
    private static final String SOUND_ALARM = "SFX_Discovery_Z4_Short";

    private EZTitles() {
    }

    public static void onMatchState(@Nonnull MatchStateEvent event) {
        var session = GauntletUtils.sessionFor(event.getSessionId()).orElse(null);
        if (!EZController.isEz(session)) return;
        switch (event.getTo()) {
            case PORTAL_OPEN -> show(session, "THE PORTAL IS OPEN", "Get to the portal!", INFO, null);
            case ACTIVE -> show(session, "MATCH STARTED", "Last team standing wins", SUCCESS, null);
            case SUDDEN_DEATH -> show(session, "SUDDEN DEATH!", "Zones close twice as fast!", DANGER, SOUND_ALARM);
            default -> {
            }
        }
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
