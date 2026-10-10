package com.gaiagauntlet.gauntlet.plugins.timer.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.hud.huds.HudWidgets;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.gamestate.utils.MatchUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.timer.utils.TimerUtils;
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
    public boolean gather(@Nonnull PlayerRef player, @Nullable GameSession session) {
        if (session == null) return false;
        var store = GameStore.withStore(player, session.getId()).orElse(null);
        if (store == null) return false; // no store found - probably not in the game
        var timer = TimerUtils.get(store).orElse(null);
        if (timer == null) return false; // no timer present, prolly not in the game either
        
        return true;
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        sent.clear();
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        if (session == null) return;
        var store = GameStore.withStore(player, session.getId()).orElse(null);
        if (store == null) return; // no store found - probably not in the game
        var timer = TimerUtils.get(store).orElse(null);
        if (timer == null) return; // no timer present, prolly not in the game either
        
        var header = header(timer.getTimerImageId());
        if (header == null) return;

        sent.background(cmd, "#TimerImage", header);
        sent.text(cmd, "#TimerLabel", TimerUtils.clock(timer.getCountdownRemaining()));
    }

    /** The header art for a phase with a countdown on the HUD, or null when the phase has none. */
    @Nullable
    private static String header(@Nonnull String imageId) {
        if (imageId == null) return null;

        return "GG/" + imageId + ".png";
    }
}
