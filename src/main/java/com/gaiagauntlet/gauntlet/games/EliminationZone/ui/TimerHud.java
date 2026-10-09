package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.huds.HudWidgets;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.plugins.auto.utils.MatchUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestate.constants.MatchState;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The countdown banner at the top of the screen: the header art for the current phase over the
 * remaining time. Shows while the lobby counts down to the portal, while the portal is open, and
 * while players wait on their platforms in the arena.
 */
public final class TimerHud implements HudElement {

    private final HudWidgets.Sent sent = new HudWidgets.Sent();

    @Nonnull @Override public String getId() {
        return "Timer";
    }

    @Nonnull @Override public String getMarkup() {
        return "Gauntlet/Games/EliminationZone/TimerHud.ui";
    }

    @Override public int getOrder() {
        return 20;
    }

    @Override
    public boolean isVisible(@Nonnull PlayerRef player, @Nullable GameSession session) {
        var match = MatchUtils.get(session);
        return EZController.isEz(session) && match != null && match.hasCountdown() && header(match.getState()) != null;
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        sent.clear();
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        var match = MatchUtils.get(session);
        if (match == null) return;
        var header = header(match.getState());
        if (header == null) return;

        sent.background(cmd, "#TimerImage", header);
        sent.text(cmd, "#TimerLabel", MatchUtils.clock(match.remainingSeconds(System.currentTimeMillis())));
    }

    /** The header art for a phase with a countdown on the HUD, or null when the phase has none. */
    @Nullable
    private static String header(@Nonnull MatchState state) {
        return switch (state) {
            case LOBBY_COUNTDOWN -> "GG/TimerHeaderUntilPortalOpens.png";
            case PORTAL_OPEN -> "GG/TimerHeaderGetToThePortal.png";
            case STAGING -> "GG/TimerHeaderMatchStartsIn.png";
            default -> null;
        };
    }
}
