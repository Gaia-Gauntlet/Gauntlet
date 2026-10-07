package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Event standings in the lobby: every team ranked by its event score, the viewer's own team
 * highlighted. Hidden until event scores are tracked.
 */
public final class ScoreboardHud implements HudElement {

    @Nonnull @Override public String getId() {
        return "Scoreboard";
    }

    @Nonnull @Override public String getMarkup() {
        return "Gauntlet/Games/EliminationZone/ScoreboardHud.ui";
    }

    @Override public int getOrder() {
        return 40;
    }

    @Override
    public boolean isVisible(@Nonnull PlayerRef player, @Nullable GameSession session) {
        return false;
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        cmd.clear("#ScoreRows");
    }
}
